package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.annotation.ComponentInfo;
import eu.mihosoft.vrl.annotation.MethodInfo;
import eu.mihosoft.vrl.annotation.ParamGroupInfo;
import eu.mihosoft.vrl.annotation.ParamInfo;
import eu.mihosoft.vrl.reflection.DefaultMethodRepresentation;
import eu.mihosoft.vrl.reflection.VisualCanvas;
import eu.mihosoft.vrl.system.VMessage;
import eu.mihosoft.vrl.types.CanvasRequest;
import eu.mihosoft.vrl.types.MethodRequest;
import eu.mihosoft.vrl.types.VisualIDRequest;
import eu.mihosoft.vrl.types.observe.VTypeObserveUtil;
import eu.mihosoft.vrl.visual.VSwingUtil;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import vtk.vtkActor;
import vtk.vtkArrowSource;
import vtk.vtkContourFilter;
import vtk.vtkDataArray;
import vtk.vtkDataSetMapper;
import vtk.vtkGlyph3D;
import vtk.vtkLookupTable;
import vtk.vtkMapper;
import vtk.vtkOutlineFilter;
import vtk.vtkPolyDataMapper;
import vtk.vtkProperty;
import vtk.vtkUnstructuredGrid;
import vtk.vtkWarpScalar;
import vtk.vtkXMLUnstructuredGridReader;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 * @author Andreas Vogel <andreas.vogel@gcsc.uni-frankfurt.de>
 * @author Christian Poliwoda <christian.poliwoda@gcsc.uni-frankfurt.de>
 */
@ComponentInfo(name = "VTUViewer", category = "VTK")
public class VTUViewer implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private boolean bUseInternFilename = false;
    private File InternFile;
    private boolean running = false;
    private transient DefaultMethodRepresentation mRep;

    // add your code here
    //@MethodInfo(hide=false, callOptions="autoinvoke")
    public Visualization visualize(
            MethodRequest mReq,
            @ParamInfo(name = "Filename", nullIsValid = true, style = "load-dialog", options = "endings=[\"vtu\"]; invokeOnChange=false") File file,
            @ParamInfo(name = "Title", style = "default", options = "invokeOnChange=true") String title,
            @ParamInfo(name = "Range", style = "selection", options = "value=[\"Auto\",\"Min/Max\"];invokeOnChange=true") String sRange,
            @ParamInfo(name = "Min", style = "default", options = "value=0;invokeOnChange=true") double minValueRange,
            @ParamInfo(name = "Max", style = "default", options = "value=1;invokeOnChange=true") double maxValueRange,
            @ParamInfo(name = "Show Data Legend", style = "default", options = "value=true;invokeOnChange=true") boolean bShowLegend,
            @ParamInfo(name = "Show Outline", style = "default", options = "value=false;invokeOnChange=true") boolean bShowOutline,
            @ParamInfo(name = "Show Orientation", style = "default", options = "invokeOnChange=true") boolean showOrientation,
            @ParamInfo(name = "Display Style", style = "selection", options = "value=[\"Surface\",\"Surface/Edge\",\"Wireframe\",\"Points\"];invokeOnChange=true") String sDisplayStyle,
            @ParamInfo(name = "Data Filter", style = "selection", options = "value=[\"None\",\"Warp (Auto)\",\"Warp (Factor)\", \"Contour\"];invokeOnChange=true") String sDataStyle,
            @ParamInfo(name = "Warp Factor", style = "default", options = "value=1;invokeOnChange=true") double warpFactor,
            @ParamInfo(name = "Num Contour", style = "default", options = "value=5;invokeOnChange=true") int numContours) {
        mRep = mReq.getMethod();

        // create vis object to add all components
        Visualization visualization = new Visualization();
        if (!title.isEmpty()) {
            visualization.setTitle(title);
        }

        // read input
        if (bUseInternFilename) {
            file = InternFile;
        }
        if (file == null) {
            return visualization;
        }
        if (!file.exists()) {
            return visualization;
        }

        String fileName = file.getAbsolutePath();//getAbsoluteFile();
        vtkXMLUnstructuredGridReader reader = new vtkXMLUnstructuredGridReader();
        reader.SetFileName(fileName);
        reader.Update();
        vtkUnstructuredGrid ug = reader.GetOutput();

        ////////////////////////////////////
        // get point Data for component
        ////////////////////////////////////
        int numVisCompData = ug.GetPointData().GetNumberOfArrays();
        if (numVisCompData != 1) {
            return visualization;
        }
//            throw new RuntimeException("Too many/few components: "+numVisCompData);
        String visCompDataName = ug.GetPointData().GetArrayName(0);
        ug.GetPointData().SetScalars(ug.GetPointData().GetArray(visCompDataName));

        ////////////////////////////////////
        // create value lookup table
        ////////////////////////////////////
        vtkLookupTable hueLut = new vtkLookupTable();
        if (sRange.equals("Auto")) {
            double[] valRange = ug.GetPointData().GetScalars().GetRange();
            minValueRange = valRange[0];
            maxValueRange = valRange[1];
        }
        hueLut.SetTableRange(minValueRange, maxValueRange);
        hueLut.SetHueRange(0.0, 1);
        hueLut.SetSaturationRange(0.6, 1);
        hueLut.SetValueRange(1, 1);
        hueLut.Build();

        if (bShowLegend) {
            visualization.setLookupTable(hueLut);
        }

        ////////////////////////////////////
        // create plain data visualization
        ////////////////////////////////////
        if (sDataStyle.equals("None")) {
            vtkLookupTable plainMapperTable = new vtkLookupTable();
            plainMapperTable.DeepCopy(hueLut);

            vtkDataSetMapper plainMapper = new vtkDataSetMapper();
            plainMapper.SetInput(ug);
            plainMapper.ScalarVisibilityOn();
            plainMapper.SetColorModeToMapScalars();
            plainMapper.SetScalarRange(plainMapperTable.GetTableRange());
            plainMapper.SetLookupTable(plainMapperTable);

            vtkActor plainActor = new vtkActor();
            plainActor.SetMapper(plainMapper);
            setDisplayStyle(plainActor, sDisplayStyle);

            visualization.addActor(plainActor);
        }

        ////////////////////////////////////
        // create warped data visualization
        ////////////////////////////////////
        if (sDataStyle.equals("Warp (Auto)") || sDataStyle.equals("Warp (Factor)")) {
            double[] valueMinMax = ug.GetPointData().GetScalars().GetRange();
            double factor = 1.0 / (valueMinMax[1] - valueMinMax[0]);
            if (sDataStyle.equals("Warp (Factor)")) {
                factor = warpFactor;
            }

            vtkWarpScalar warpScalar = new vtkWarpScalar();
            warpScalar.SetInput(ug);
            warpScalar.SetScaleFactor(factor);
            warpScalar.Update();

            vtkLookupTable warpTable = new vtkLookupTable();
            warpTable.DeepCopy(hueLut);

            vtkDataSetMapper warpMapper = new vtkDataSetMapper();
            warpMapper.SetInputConnection(warpScalar.GetOutputPort());
            warpMapper.SetScalarRange(warpTable.GetTableRange());
            warpMapper.SetLookupTable(warpTable);

            vtkActor warpActor = new vtkActor();
            warpActor.SetMapper(warpMapper);
            setDisplayStyle(warpActor, sDisplayStyle);

            visualization.addActor(warpActor);
        }

        ////////////////////////////////////
        // create contour filter
        ////////////////////////////////////
        if (sDataStyle.equals("Contour")) {

            vtkLookupTable contTable = new vtkLookupTable();
            contTable.DeepCopy(hueLut);

            vtkContourFilter contours = new vtkContourFilter();
            contours.SetInput(ug);
            contours.GenerateValues(numContours, contTable.GetTableRange());

            vtkPolyDataMapper contMapper = new vtkPolyDataMapper();
            contMapper.SetInput(contours.GetOutput());
            contMapper.SetScalarRange(contTable.GetTableRange());
            contMapper.SetLookupTable(contTable);

            vtkActor contActor = new vtkActor();
            contActor.SetMapper(contMapper);
            setDisplayStyle(contActor, sDisplayStyle);

            visualization.addActor(contActor);
        }


        ////////////////////////////////////
        // create outline
        ////////////////////////////////////
        if (bShowOutline) {
            vtkOutlineFilter outline = new vtkOutlineFilter();
            outline.SetInput(ug);

            vtkPolyDataMapper outlineMapper = new vtkPolyDataMapper();
            outlineMapper.SetInput(outline.GetOutput());

            vtkActor outlineActor = new vtkActor();
            outlineActor.SetMapper(outlineMapper);

            visualization.addActor(outlineActor);
        }

        visualization.setOrientationVisible(showOrientation);

        return visualization;
    }

    private void setDisplayStyle(vtkActor actor, String style) {

//        String msg = getClass().getSimpleName()
//                + ".setDisplayStyle(vtkActor actor, String style).\n"
//                + "actor = " + actor.GetClassName() + "\n"
//                + "style = " + style;
//        System.out.println(msg);
//        VMessage.info("debug-info", msg);

        if (style.equals("Surface")) {
            actor.GetProperty().SetRepresentationToSurface();
        } else if (style.equals("Surface/Edge")) {
            actor.GetProperty().SetRepresentationToSurface();
            actor.GetProperty().EdgeVisibilityOn();
        } else if (style.equals("Wireframe")) {
            actor.GetProperty().SetRepresentationToWireframe();
            actor.GetProperty().SetAmbient(1.0);
            actor.GetProperty().SetDiffuse(0.0);
            actor.GetProperty().SetSpecular(0.0);
        } else if (style.equals("Points")) {
            actor.GetProperty().SetRepresentationToPoints();
            actor.GetProperty().SetPointSize(2.5);
        } else if (style.equals("Vectorfield")) {
//            actor.GetProperty().SetRepresentationToPoints();
//            actor.ApplyProperties();                    
        } else {
            throw new RuntimeException("Style not found.");
        }
    }

    @MethodInfo(valueName = "VTU-File", valueStyle = "silent")
    public java.io.File start(
            CanvasRequest cReq,
            VisualIDRequest idReq,
            @ParamInfo(style = "save-folder-dialog") final File dir,
            @ParamInfo(name = "filename begin", options = "value=\"data\"") final String startsWith) {

        final String ending = "vtu";

        VisualCanvas mainCanvas = cReq.getCanvas();


        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".vtu")) {
                f.delete();
            }

        }

        running = true;
        bUseInternFilename = true;


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                File lastFile = null;
                while (running) {
                    try {
                        File f = getLastFileInFolder(dir, startsWith, ending);

                        if (!f.equals(lastFile)) {
                            InternFile = f;

                            /*mainCanvas.getInspector().invokeFromInvokeButton(
                             mRep.getDescription(),
                             idReq.getID()
                             );*/

                            mRep.invokeAsCallParent(false);
                        }
                        lastFile = f;

                    } catch (Exception ex) {
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(VTUViewer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }
        });

        t.start();


        String tmp = dir.getAbsolutePath() + "/" + startsWith + ".vtu";
        tmp.replace("//", "/");
        return new File(tmp);
    }

    @MethodInfo()
    public void stop() {
        running = false;
        bUseInternFilename = false;
    }

    private File getLastFileInFolder(@ParamInfo(style = "load-folder-dialog") File dir,
            @ParamInfo(name = "beginning, e.g. \"file00\"") final String startsWith,
            @ParamInfo(name = "ending, e.g. \"vtu\"") final String ending) {

        ArrayList<File> result = getAllFilesInFolder(dir, startsWith, ending);

        return result.get(result.size() - 1);
    }

    private File getFirstFileInFolder(@ParamInfo(style = "load-folder-dialog") File dir,
            @ParamInfo(name = "beginning, e.g. \"file00\"") final String startsWith,
            @ParamInfo(name = "ending, e.g. \"vtu\"") final String ending) {

        ArrayList<File> result = getAllFilesInFolder(dir, startsWith, ending);

        return result.get(0);
    }

    private ArrayList<File> getAllFilesInFolder(@ParamInfo(style = "load-folder-dialog") File dir,
            @ParamInfo(name = "beginning, e.g. \"file00\"") final String startsWith,
            @ParamInfo(name = "ending, e.g. \"vtu\"") final String ending) {

        ArrayList<File> result = new ArrayList<File>();

        if (dir != null && dir.isDirectory()) {
            for (File f : dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathName) {
                    boolean fileAccept = pathName.getName().toLowerCase().endsWith("." + ending) || pathName.isDirectory();

                    int dot = pathName.getPath().lastIndexOf(".");
                    int sep = pathName.getPath().lastIndexOf(File.separator);

                    String fileName = pathName.getPath().substring(sep + 1, dot);

                    boolean nameAccept = startsWith.equals("") || fileName.startsWith(startsWith);

                    return fileAccept && nameAccept;
                }
            }))// fore end 
            {
                if (f.isFile()) {
                    result.add(f);
                }
            }

        } // if a file is directly choosen, add it
        else if (dir != null && dir.isFile()) {
            result.add(dir);
        }

//        else {
//            //
//            throw new RuntimeException("VTUViewer.getAllFilesInFolder(): path '" + dir.getName() + "' not found.");
//        }

        return result;
    }

//    @MethodInfo( valueStyle="default")
    public Visualization visualizeAll(
            MethodRequest mReq,
            @ParamGroupInfo(group = "Files|false|File depending data.")
            @ParamInfo(name = "folderWithPlotFiles",
            style = "observe-load-dialog",
            options = "fileAnalyzer=\"VTUAnalyzer\";tag=\"element\"") File folder,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "beginning, e.g. \"file00\"") String startsWith,
            //            @ParamGroupInfo(group = "Files")
            //            @ParamInfo(name = "elementInFile", options = "value=0")
            //                    int elementInFile,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "elementInFile",
            style = "observe-load-dialog",
            options = "fileAnalyzer=\"VTUAnalyzer\";tag=\"element\"") String elementInFile,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "makePNG", options = "value=false")
            final boolean makePNG,
            @ParamGroupInfo(group = "Plot|false|Plot depending data.")
            @ParamInfo(name = "Title", style = "default") String title,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "waiting in ms", options = "value=0")
            final long wait,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Range",
            style = "selection",
            options = "value=[\"Auto\",\"Min/Max\"]") String sRange,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Min", style = "default", options = "value=0") double minValueRange,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Max", style = "default", options = "value=1") double maxValueRange,
            //            @ParamGroupInfo(group = "Plot")
            //            @ParamInfo(name = "Show Data Legend", style = "default", options = "value=true; invokeOnChange=true") 
            //                    boolean bShowLegend,
            //            @ParamGroupInfo(group = "Plot")
            //            @ParamInfo(name = "Show Outline", style = "default", options = "value=false; invokeOnChange=true")
            //                    boolean bShowOutline,
            //            @ParamGroupInfo(group = "Plot")
            //            @ParamInfo(name = "Show Orientation", style = "default", options = "invokeOnChange=true")
            //                    boolean showOrientation,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Show Data Legend", style = "default", options = "value=true") boolean bShowLegend,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Show Outline", style = "default", options = "value=false") boolean bShowOutline,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Show Orientation", style = "default") boolean showOrientation,
            @ParamGroupInfo(group = "Filters|false|Choose which filter should be used.")
            @ParamInfo(name = "Display Style", style = "selection",
            options = "value=[\"Surface\",\"Surface/Edge\",\"Wireframe\",\"Points\", \"Vectorfield\"]") String sDisplayStyle,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Data Filter", style = "selection",
            options = "value=[\"None\",\"Warp (Auto)\",\"Warp (Factor)\", \"Contour\"]") String sDataStyle,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Warp Factor", style = "default", options = "value=1") double warpFactor,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Num Contour", style = "default", options = "value=5") int numContours,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "scale factor", style = "default", options = "value=0.05") double scaleFactor) {

        mRep = mReq.getMethod();


//        // read input
//        if (bUseInternFilename) {
//            folder = InternFile;
//        }
//
//        if ((folder == null) || (!folder.exists())) {
//            return new Visualization();
//        }


        ArrayList<File> allFiles = getAllFilesInFolder(folder, startsWith, "vtu");

        System.out.println("-- allFiles.size() = " + allFiles.size());

        Visualization lastVisualization = new Visualization();

        for (File file : allFiles) {

            // create vis object to add all components
            final Visualization visualization = new Visualization();

            visualization.setOrientationVisible(showOrientation);

            if (title.isEmpty()) {
//                visualization.setTitle(file.getName());
            } else {
                visualization.setTitle(title);
            }

            final String fileName = file.getAbsolutePath();//getAbsoluteFile();
//            System.out.println("-- fileName = "+ fileName);

            vtkXMLUnstructuredGridReader reader = new vtkXMLUnstructuredGridReader();
            reader.SetFileName(fileName);
            reader.Update();

            vtkUnstructuredGrid ug = reader.GetOutput();


            settingScalarsOrVectors(sDisplayStyle, ug, elementInFile);

            ////////////////////////////////////
            // get point Data for component
            ////////////////////////////////////
//            int numVisCompData = ug.GetPointData().GetNumberOfArrays();

//            System.out.println("ELEMENTS/ARRAYS IN FILE:");
//            for (int i = 0; i < numVisCompData; i++) {
//                System.out.println(i + ") " + ug.GetPointData().GetArrayName(i));
//            }
//
//            System.out.println("Components IN FILE:");
//            for (int i = 0; i < ug.GetPointData().GetNumberOfComponents(); i++) {
//                System.out.println("(" + i + ",0) " + ug.GetPointData().GetComponent(i, 0));
//            }
//
//            System.out.println("Tuples IN FILE: " + ug.GetPointData().GetNumberOfTuples());

//            if (numVisCompData < elementInFile) {
//                String msg = "There are only " + numVisCompData + " elements in the selected file."
//                        + " And you want to select: " + elementInFile;
//
//                VMessage.error("Wrong Parameter", msg);
//
//                System.err.println(msg);
//
//                VMessage.info("Hint", "Notice that if you want to get the first element"
//                        + "in file you need to tip 0 (zero).");
//
//                return visualization;
//            }
//            
//
//            String visCompDataName = ug.GetPointData().GetArrayName(elementInFile);
//            ug.GetPointData().SetScalars(ug.GetPointData().GetArray(visCompDataName));
//            
//            VMessage.info("visCompDataName is " + elementInFile + " elementInFile", visCompDataName);

            ////////////////////////////////////
            // create value lookup table
            ////////////////////////////////////
            vtkLookupTable defaultLookupTable = createLookupTable(
                    sRange, sDisplayStyle, sDataStyle, ug,
                    minValueRange, maxValueRange,
                    bShowLegend, visualization, elementInFile);

            ////////////////////////////////////
            // create plain data visualization
            ////////////////////////////////////
            if (sDataStyle.equals("None")) {
                createPlainDataVisualization(
                        defaultLookupTable, ug,
                        sDisplayStyle, visualization);
            }
            ////////////////////////////////////
            // create warped data visualization
            ////////////////////////////////////
            if (sDataStyle.equals("Warp (Auto)") || sDataStyle.equals("Warp (Factor)")) {

                createWarpDataVisualization(ug, sDataStyle, warpFactor,
                        defaultLookupTable, sDisplayStyle, visualization, elementInFile);
            }

            ////////////////////////////////////
            // create contour filter
            ////////////////////////////////////
            if (sDataStyle.equals("Contour")) {

                createContourFilter(defaultLookupTable, ug,
                        numContours, sDisplayStyle, visualization);
            }

            ////////////////////////////////////
            // create vector field filter
            ////////////////////////////////////
            if (sDisplayStyle.equals("Vectorfield")) {

                createVectorFieldFilter(defaultLookupTable, ug,
                        startsWith, elementInFile, scaleFactor,
                        sDisplayStyle, visualization);
            }


            ////////////////////////////////////
            // create outline
            ////////////////////////////////////
            if (bShowOutline) {

                createOutlineFilter(ug, visualization);
            }


            if (makePNG) {
                makePNGs(visualization, fileName, wait);
            }

            lastVisualization = visualization;

        }

        return lastVisualization;
    }

    /**
     * create default value lookup table.
     *
     * @param sRange
     * @param ug
     * @param minValueRange
     * @param maxValueRange
     * @param bShowLegend
     * @param visualization
     * @return
     */
    private vtkLookupTable createLookupTable(String sRange, String sDisplayStyle, String sDataStyle,
            vtkUnstructuredGrid ug, double minValueRange, double maxValueRange,
            boolean bShowLegend, final Visualization visualization, String elementInFile) {

        vtkLookupTable defaultLookupTable = new vtkLookupTable();

//        if (sRange.equals("Auto") && ug.GetPointData().GetScalars() != null) {
//
//            double[] valRange = ug.GetPointData().GetScalars().GetRange();
//
//            minValueRange = valRange[0];
//            maxValueRange = valRange[1];
//
//        } else if (sRange.equals("Auto") && ug.GetPointData().GetVectors() != null) {
//
//            double[] valRange = ug.GetPointData().GetVectors().GetRange();
//
//            minValueRange = valRange[0];
//            maxValueRange = valRange[1];
//        }

//        settingScalarsOrVectors(sDataStyle, ug, elementInFile);

        double[] valRange;

        if (sRange.equals("Auto")) {

            valRange = getRange(sDisplayStyle, ug, elementInFile);

            minValueRange = valRange[0];
            maxValueRange = valRange[1];


//            VMessage.info(getClass().getSimpleName() + ".createLookupTable()",
//                    " sRange == Auto \n"
//                    + defaultLookupTable.Print());
        }

        if (!sDisplayStyle.equals("Vectorfield")) {

            defaultLookupTable.SetTableRange(minValueRange, maxValueRange);
            defaultLookupTable.SetHueRange(0.0, 1);
            defaultLookupTable.SetSaturationRange(0.6, 1);
            defaultLookupTable.SetValueRange(1, 1);


            VMessage.info(getClass().getSimpleName() + ".createLookupTable()",
                    "sDataStyle != Vectorfield  \n"
                    + defaultLookupTable.Print());

        } else if (sDisplayStyle.equals("Vectorfield") && sDataStyle.equals("None")) {


//            defaultLookupTable.SetTableRange(0.0, 0.0);
            defaultLookupTable.SetHueRange(0.0, 1);
//            defaultLookupTable.SetSaturationRange(0.0, 0.0);
//            defaultLookupTable.SetValueRange(0.0, 0.0);
//
//            defaultLookupTable.SetScale(0);
//            defaultLookupTable.SetAlphaRange(0, 0);
            
            defaultLookupTable.SetNanColor(0, 0, 0, 0);

            VMessage.info(getClass().getSimpleName() + ".createLookupTable()",
                    "sDataStyle == Vectorfield && sRange == None \n"
                    + defaultLookupTable.Print());


        }

        defaultLookupTable.Build();

//        VMessage.info(getClass().getSimpleName()+".createLookupTable()",
//                    "sdefaultLookupTable.Build() \n"
//                    + defaultLookupTable.Print() );


        if (bShowLegend) {
            visualization.setLookupTable(defaultLookupTable);
        }
        return defaultLookupTable;
    }

    /**
     * create plain data visualization.
     *
     * @param sDataStyle
     * @param defaultLookupTable
     * @param ug
     * @param sDisplayStyle
     * @param visualization
     */
    private void createPlainDataVisualization(
            vtkLookupTable defaultLookupTable, vtkUnstructuredGrid ug,
            String sDisplayStyle, final Visualization visualization) {

        vtkLookupTable plainLookupTable = new vtkLookupTable();
        plainLookupTable.DeepCopy(defaultLookupTable);

        vtkDataSetMapper plainMapper = new vtkDataSetMapper();
        plainMapper.SetInput(ug);

//        plainMapper.ScalarVisibilityOn();
//        plainMapper.SetColorModeToMapScalars();
//        plainMapper.SetScalarRange(plainLookupTable.GetTableRange());
//        plainMapper.SetLookupTable(plainLookupTable);

        settingsForMappers(sDisplayStyle, plainMapper, plainLookupTable);

        vtkActor plainActor = new vtkActor();
        plainActor.SetMapper(plainMapper);

        setDisplayStyle(plainActor, sDisplayStyle);

        visualization.addActor(plainActor);

    }

    /**
     * create warped data visualization.
     *
     * @param ug
     * @param sDataStyle
     * @param warpFactor
     * @param defaultLookupTable
     * @param sDisplayStyle
     * @param visualization
     */
    private void createWarpDataVisualization(vtkUnstructuredGrid ug,
            String sDataStyle, double warpFactor,
            vtkLookupTable defaultLookupTable, String sDisplayStyle,
            final Visualization visualization, String elementInFile) {

//        double[] valueMinMax = ug.GetPointData().GetScalars().GetRange();
        double[] valueMinMax = getRange(sDisplayStyle, ug, elementInFile);

        double factor = 1.0 / (valueMinMax[1] - valueMinMax[0]);

        if (sDataStyle.equals("Warp (Factor)")) {
            factor = warpFactor;
        }

        vtkWarpScalar warpScalar = new vtkWarpScalar();
        warpScalar.SetInput(ug);
        warpScalar.SetScaleFactor(factor);
        warpScalar.Update();

        vtkLookupTable warpTable = new vtkLookupTable();
        warpTable.DeepCopy(defaultLookupTable);

        vtkDataSetMapper warpMapper = new vtkDataSetMapper();
        warpMapper.SetInputConnection(warpScalar.GetOutputPort());

//        warpMapper.SetScalarRange(warpTable.GetTableRange());
//        warpMapper.SetLookupTable(warpTable);

        settingsForMappers(sDisplayStyle, warpMapper, warpTable);

        vtkActor warpActor = new vtkActor();
        warpActor.SetMapper(warpMapper);

        setDisplayStyle(warpActor, sDisplayStyle);

        visualization.addActor(warpActor);
    }

    /**
     *
     * @param defaultLookupTable
     * @param ug
     * @param numContours
     * @param sDisplayStyle
     * @param visualization
     */
    private void createContourFilter(vtkLookupTable defaultLookupTable,
            vtkUnstructuredGrid ug, int numContours, String sDisplayStyle,
            final Visualization visualization) {

        vtkLookupTable contourTable = new vtkLookupTable();
        contourTable.DeepCopy(defaultLookupTable);

        vtkContourFilter contours = new vtkContourFilter();
        contours.SetInput(ug);
        contours.GenerateValues(numContours, contourTable.GetTableRange());

        vtkPolyDataMapper contourMapper = new vtkPolyDataMapper();
        contourMapper.SetInput(contours.GetOutput());

//        contourMapper.SetScalarRange(contourTable.GetTableRange());
//        contourMapper.SetLookupTable(contourTable);

        settingsForMappers(sDisplayStyle, contourMapper, contourTable);

        vtkActor contourActor = new vtkActor();
        contourActor.SetMapper(contourMapper);

        setDisplayStyle(contourActor, sDisplayStyle);

        visualization.addActor(contourActor);
    }

    /**
     *
     * @param defaultLookupTable
     * @param ug
     * @param startsWith
     * @param elementInFile
     * @param scaleFactor
     * @param sDisplayStyle
     * @param visualization
     */
    private void createVectorFieldFilter(vtkLookupTable defaultLookupTable,
            vtkUnstructuredGrid ug, String startsWith,
            String elementInFile, double scaleFactor,
            String sDisplayStyle, final Visualization visualization) {

        vtkLookupTable vectorfieldTable = new vtkLookupTable();
        vectorfieldTable.DeepCopy(defaultLookupTable);

//                reader.SetFileName(file.getAbsolutePath());
//                reader.Update();
////                vtkUnstructuredGrid image = reader.GetOutput();
//                ug = reader.GetOutput();
//                ug.GetPointData().SetVectors(ug.GetPointData().GetArray(elementInFile));

        // represent vector field
        vtkGlyph3D vectorGlyph = new vtkGlyph3D();
        vtkArrowSource arrowSource = new vtkArrowSource();

//        int n = ug.GetPointData().GetNumberOfArrays();
//        for (int i = 0; i < n; i++) {
//            System.out.println("name of array[" + i + "]: " + ug.GetPointData().GetArrayName(i));
//        }

        vectorGlyph.SetInputConnection(ug.GetProducerPort());
        vectorGlyph.SetSourceConnection(arrowSource.GetOutputPort());

        vectorGlyph.SetScaleModeToScaleByVector();
        vectorGlyph.SetVectorModeToUseVector();
        vectorGlyph.ScalingOn();
        vectorGlyph.OrientOn();
        vectorGlyph.SetColorModeToColorByVector();//color the glyphs
        vectorGlyph.SetScaleFactor(scaleFactor);
//        vectorGlyph.SetScaleModeToDataScalingOff(); // all glyphs have same lenght 

        VTUAnalyzer analyzer = (VTUAnalyzer) VTypeObserveUtil.
                getFileAnanlyzerByName(VTUAnalyzer.class.getSimpleName());
        analyzer.setStartsWith(startsWith);

//        System.out.println("elementInFile = " + elementInFile);
//        System.out.println("analyser = " + analyzer);
//        System.out.println("analyser.getFileEntries().size() = " + analyzer.getFileEntries().size());
//        for (int i = 0; i < analyzer.getFileEntries().size(); i++) {
//            System.out.println(i + ") " + analyzer.getFileEntries().get(i));
//        }

        int index = analyzer.getFileEntries().indexOf(elementInFile);


        vectorGlyph.SetInputArrayToProcess(
                //  elementInFile,
                index,
                ug.GetInformation());

        vectorGlyph.Update();


        vtkPolyDataMapper vectorGlyphMapper = new vtkPolyDataMapper();
        vectorGlyphMapper.SetInputConnection(vectorGlyph.GetOutputPort());

//        vectorGlyphMapper.ScalarVisibilityOn();//color for glyphs
//        vectorGlyphMapper.SetLookupTable(vectorfieldTable);// glyphs values/colors are orientated value range
//        vectorGlyphMapper.Update();

        settingsForMappers(sDisplayStyle, vectorGlyphMapper, vectorfieldTable);

        vtkActor vectorActor = new vtkActor();
        vectorActor.SetMapper(vectorGlyphMapper);
        
        //optional setttings for lightning
        vtkProperty sliceProp = new vtkProperty();
        sliceProp.SetDiffuse(0.0);
        sliceProp.SetSpecular(0.0);
        sliceProp.SetAmbient(1.0);

        vectorActor.SetProperty(sliceProp);
        

        setDisplayStyle(vectorActor, sDisplayStyle);

        visualization.addActor(vectorActor);
    }

    /**
     *
     * @param ug
     * @param visualization
     */
    private void createOutlineFilter(vtkUnstructuredGrid ug,
            final Visualization visualization) {

        vtkOutlineFilter outline = new vtkOutlineFilter();
        outline.SetInput(ug);

        vtkPolyDataMapper outlineMapper = new vtkPolyDataMapper();
        outlineMapper.SetInput(outline.GetOutput());

        vtkActor outlineActor = new vtkActor();
        outlineActor.SetMapper(outlineMapper);

        visualization.addActor(outlineActor);
    }

    /**
     *
     * @param visualization
     * @param makePNG
     * @param fileName
     * @param wait
     */
    private void makePNGs(final Visualization visualization,
            final String fileName, final long wait) {

        VSwingUtil.invokeAndWait(new Runnable() {
            public void run() {
                mRep.getReturnValue().emptyView();
                mRep.getReturnValue().setViewValue(visualization);

                String[] split = fileName.split(".vtu");
                File pngFile = new File(split[0] + ".png");
                ((VTKOutputType) mRep.getReturnValue()).saveImage(pngFile);

                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ex) {
//                     Logger.getLogger(VTUViewer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    /**
     *
     * @param ug
     * @param elementInFile
     * @return
     */
    private vtkDataArray getElementInFileID(vtkUnstructuredGrid ug, String elementInFile) {

        vtkDataArray id = ug.GetPointData().GetArray(elementInFile);

        if (id == null) {

            String msg = getClass().getSimpleName() + ".getElementInFileID() \n"
                    + "id == null";

            System.err.println(msg);

            VMessage.warning("null", msg);

            throw new NullPointerException(msg);
        }

        return id;
    }

    /**
     *
     * @param sDisplayStyle
     * @param ug
     * @param elementInFile
     */
    private void settingScalarsOrVectors(String sDisplayStyle,
            vtkUnstructuredGrid ug,
            String elementInFile) {

        vtkDataArray id = getElementInFileID(ug, elementInFile);

//        System.out.println("settingScalarsOrVectors(): \n"
//                + "sDisplayStyle.equals(\"Vectorfield\") = "
//                + sDisplayStyle.equals("Vectorfield"));

        if (sDisplayStyle.equals("Vectorfield")) {

            ug.GetPointData().SetVectors(id);
//            ug.GetPointData().SetActiveVectors(elementInFile);

        } else {

            ug.GetPointData().SetScalars(id);
//            ug.GetPointData().SetActiveScalars(elementInFile);
        }
    }

    /**
     *
     * @param sDisplayStyle
     * @param ug
     * @param elementInFile
     * @return
     */
    private double[] getRange(String sDisplayStyle,
            vtkUnstructuredGrid ug,
            String elementInFile) {

        settingScalarsOrVectors(sDisplayStyle, ug, elementInFile);

        double[] valueMinMax;
        vtkDataArray dataArray;

        if (sDisplayStyle.equals("Vectorfield")) {

            dataArray = ug.GetPointData().GetVectors();


        } else {

            dataArray = ug.GetPointData().GetScalars();
        }


//            System.out.println("ug = "+ ug);
//            System.out.println("ug.GetPointData() = "+ ug.GetPointData());
        System.out.println("dataArray = " + dataArray);

        if (dataArray != null) {
            valueMinMax = dataArray.GetRange();
            
        } else {
            VMessage.exception("ERROR", "The choosen parameter combination led to"
                    + " an invalid state. Please check your selection. \n"
                    + " E.g: select only vector field as DisplayStyle if the a"
                    + " vector field is selected from a vtu file.");
            
            valueMinMax = null;
        }

        return valueMinMax;
    }

    private void settingsForMappers(String sDisplayStyle, //vtkUnstructuredGrid ug,
            vtkMapper mapper, vtkLookupTable lookupTable) {

//        mapper.SetInputConnection(ug.GetProducerPort()); // DONT DO THESE HERE


        mapper.SetLookupTable(lookupTable);

        if (!sDisplayStyle.equals("Vectorfield")) {

            mapper.SetScalarRange(lookupTable.GetTableRange());
            mapper.ScalarVisibilityOn();
            mapper.SetColorModeToMapScalars();

        } else if (sDisplayStyle.equals("Vectorfield")) {

            mapper.SetScalarRange(lookupTable.GetTableRange());

//            mapper.ScalarVisibilityOff();
            mapper.ScalarVisibilityOn();//color for glyphs


        }

        mapper.Update();
    }
}
