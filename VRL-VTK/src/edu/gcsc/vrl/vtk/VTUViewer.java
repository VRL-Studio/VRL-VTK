package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.annotation.ComponentInfo;
import eu.mihosoft.vrl.annotation.MethodInfo;
import eu.mihosoft.vrl.annotation.OutputInfo;
import eu.mihosoft.vrl.annotation.ParamGroupInfo;
import eu.mihosoft.vrl.annotation.ParamInfo;
import eu.mihosoft.vrl.reflection.DefaultMethodRepresentation;
import eu.mihosoft.vrl.reflection.TypeRepresentationBase;
import eu.mihosoft.vrl.reflection.VisualCanvas;
import eu.mihosoft.vrl.reflection.VisualObject;
import eu.mihosoft.vrl.system.VMessage;
import eu.mihosoft.vrl.types.MethodRequest;
import eu.mihosoft.vrl.types.MultipleOutputType;
import eu.mihosoft.vrl.types.SelectionInputType;
import eu.mihosoft.vrl.types.observe.LoadFileObservable;
import eu.mihosoft.vrl.types.observe.LoadObserveFileType;
import eu.mihosoft.vrl.types.observe.VTypeObserveUtil;
import eu.mihosoft.vrl.visual.VSwingUtil;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    public static class Range {

        private static final String AUTO = "Auto";
        private static final String MIN_MAX = "Min/Max";
    }

    public static class DisplayStyle {

        private static final String SURFACE = "Surface";
        private static final String SURFACE_EDGE = "Surface/Edge";
        private static final String WIREFRAME = "Wireframe";
        private static final String POINTS = "Points";
        private static final String VECTORFIELD = "Vectorfield";
    }

    public static class DataStyle {

        private static final String NONE = "None";
        private static final String WARP_AUTO = "Warp (Auto)";
        private static final String WARP_FACTOR = "Warp (Factor)";
        private static final String CONTOUR = "Contour";
    }
    private transient Visualization lastVisualization = new Visualization();
    private transient Thread thread = null;
    private transient ArrayList<File> lastAllFiles = new ArrayList<File>();
    private transient DefaultMethodRepresentation mRep;

    protected Thread getThread() {
        synchronized (this) {
            return thread;
        }
    }

    protected void setThread(Thread thread) {
        synchronized (this) {
            this.thread = thread;
        }
    }

    protected boolean isAlive() {
        synchronized (this) {
            if (this.thread != null) {
                return this.thread.isAlive();
            } else {
                return false;
            }
        }
    }

    protected void startThread() {
        synchronized (this) {
            if (this.thread != null) {
                this.thread.start();
            }
        }
    }

    protected void interruptThread() {
        synchronized (this) {
            if (this.thread != null) {
                this.thread.interrupt();
            }
        }
    }

    @OutputInfo(style = "multi-out",
    elemStyles = {"default", "silent"}, elemNames = {"", "File"},
    elemTypes = {Visualization.class, File.class})
    @MethodInfo(hide = false, hideCloseIcon = true, valueStyle = "multi-out")
    public Object[] visualize(
            MethodRequest mReq,
            @ParamGroupInfo(group = "Files|false|File depending data.")
            @ParamInfo(name = "Folder or File",
            style = "observe-load-dialog",
            options = "tag=\"element\"") final File fileOrFolder,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "File Beginning",
            nullIsValid = true) final String startsWith,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "Data Component",
            style = "observe-load-dialog",
            options = "fileAnalyzer=\"VTUAnalyzer\";tag=\"element\"") final String elemInFile,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "Produce PNG", options = "value=false")
            final boolean makePNG,
            @ParamGroupInfo(group = "Plot|false|Plot depending data.")
            @ParamInfo(name = "Title", style = "default") final String title,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Frame Duration (in ms)", options = "value=0")
            final long wait,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Range",
            style = "selection",
            options = "value=[\""
            + VTUViewer.Range.AUTO + "\",\""
            + VTUViewer.Range.MIN_MAX + "\"]") final String sRange,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Min",
            style = "default",
            options = "value=0",
            nullIsValid = true) final double minValueRange,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Max",
            style = "default",
            options = "value=1",
            nullIsValid = true) final double maxValueRange,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Show Data Legend", style = "default", options = "value=true") final boolean bShowLegend,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Show Outline", style = "default", options = "value=false") final boolean bShowOutline,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Show Orientation", style = "default") final boolean showOrientation,
            @ParamGroupInfo(group = "Filters|false|Choose which filter should be used.")
            @ParamInfo(name = "Display Style", style = "selection",
            options = "value=[\""
            + VTUViewer.DisplayStyle.SURFACE + "\",\""
            + VTUViewer.DisplayStyle.SURFACE_EDGE + "\",\""
            + VTUViewer.DisplayStyle.WIREFRAME + "\",\""
            + VTUViewer.DisplayStyle.POINTS + "\",\""
            + VTUViewer.DisplayStyle.VECTORFIELD + "\"]") final String sDisplayStyle,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Data Filter", style = "selection",
            options = "value=[\""
            + VTUViewer.DataStyle.NONE + "\",\""
            + VTUViewer.DataStyle.WARP_AUTO + "\",\""
            + VTUViewer.DataStyle.WARP_FACTOR + "\",\""
            + VTUViewer.DataStyle.CONTOUR + "\"]") final String sDataStyle,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Warp Factor",
            style = "default",
            options = "value=1",
            nullIsValid = true) final double warpFactor,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Num Contour",
            style = "default",
            options = "value=5",
            nullIsValid = true) final int numContours,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Vector Field Scale Factor",
            style = "default",
            options = "value=0.05",
            nullIsValid = true) final double fieldScaleFactor) {


        mRep = mReq.getMethod();

        final VTUAnalyzer analyzer = (VTUAnalyzer) VTypeObserveUtil.getFileAnanlyzerByClass(VTUAnalyzer.class);
        analyzer.setStartsWith(startsWith);

        final int id = mRep.getParentObject().getObjectID();
        final Object o = ((VisualCanvas) mRep.getMainCanvas()).getInspector().getObject(id);
        final int windowID = 0;
        final String tag = "element";

        lastVisualization = new Visualization();

        // stop current thread iff running
        if (getThread() != null) {
            interruptThread();
            while (getThread() != null && isAlive()) {
                // wait
            }
            System.out.println("Setting running to false and INTERRUPT");
        }

        lastAllFiles = new ArrayList<File>();
        System.out.println("Setting running to true");

        // start new vis thread
        setThread(new Thread(new Runnable() {

            @Override
            public void run() {
                System.out.println(" - - Start Thread");
                try {
                    boolean filesAnalysed = false;
                    int waitingTime = 100;
                    String elementInFile = elemInFile;
                    int index = 0;
                    
                    while (true) {

                        ArrayList<File> allFiles = getAllFilesInFolder(fileOrFolder, startsWith, "vtu");

                        // if nothing changed, wait 1s before next lookup
                        if (lastAllFiles.equals(allFiles)) {
                            System.out.println("WAIT BEFORE FILELOOP BEGIN");
                            Thread.sleep(waitingTime);
                            System.out.println("WAIT BEFORE FILELOOP END");
                        }

                        if (!filesAnalysed && !allFiles.isEmpty()) {
                            System.out.println(" ***** ANALYSE FILE ***** ");
                            File file = allFiles.get(0);
                            if (!file.getAbsolutePath().isEmpty() && file.exists()) {
                                LoadFileObservable.getInstance().setSelectedFile(
                                        file, tag, o, windowID);

                                if (elementInFile.equals("")) {
                                    ArrayList<String> list = new ArrayList<String>();
                                    list.addAll(analyzer.analyzeFile(file));
                                    if (!list.isEmpty()) {
                                        elementInFile = list.get(0);
                                        index = list.indexOf(elementInFile);
                                    }
                                }
                            } else {
                                LoadFileObservable.getInstance().setInvalidFile(tag, o, windowID);
                            }

                            filesAnalysed = true;
                            waitingTime = 1000;
                        }

                        System.out.println(" - - Loop Files in Thread");

                        for (File file : allFiles) {

                            if (lastAllFiles.contains(file)) {
                                continue;
                            }

                            System.out.println(" - - PLOT FILE");

                            // create vis object to add all components
                            final Visualization visualization = new Visualization();

                            visualization.setOrientationVisible(showOrientation);

                            if (title.isEmpty()) {
                            } else {
                                visualization.setTitle(title);
                            }
                        
                            final String fileName = file.getAbsolutePath();//getAbsoluteFile();

                            vtkXMLUnstructuredGridReader reader = new vtkXMLUnstructuredGridReader();
                            reader.SetFileName(fileName);
                            reader.Update();

                            vtkUnstructuredGrid ug = reader.GetOutput();


                            if (settingScalarsOrVectors(sDisplayStyle, ug, elementInFile) == false) {
                                continue;
                            }

                            vtkLookupTable defaultLookupTable = createLookupTable(
                                    sRange, sDisplayStyle, sDataStyle, ug,
                                    minValueRange, maxValueRange,
                                    bShowLegend, visualization, elementInFile);

                            if (sDataStyle.equals(DataStyle.NONE)) {

                                System.out.println(" - - DataStyle.NONE ");

                                // TODO FIX THIS WORKAROUND if() else()
                                // if DataStyle.NONE and DisplayStyle.VECTORFIELD are chosen
                                // there is a plain visualized too much
                                if (sDisplayStyle.equals(DisplayStyle.VECTORFIELD)) {

                                    System.out.println(" - - DataStyle.NONE "
                                            + " && DisplayStyle.VECTORFIELD WORKAROUND");

                                    createContourFilter(
                                            defaultLookupTable, ug, 0,
                                            sDisplayStyle, visualization);

                                } else {

                                    createPlainDataVisualization(
                                            defaultLookupTable, ug,
                                            sDisplayStyle, visualization);
                                }
                            }

                            if (sDataStyle.equals(DataStyle.WARP_AUTO)
                                    || sDataStyle.equals(DataStyle.WARP_FACTOR)) {

                                createWarpDataVisualization(ug, sDataStyle, warpFactor,
                                        defaultLookupTable, sDisplayStyle, visualization, elementInFile);
                            }


                            if (sDataStyle.equals(DataStyle.CONTOUR)) {

                                createContourFilter(defaultLookupTable, ug,
                                        numContours, sDisplayStyle, visualization);
                            }

                            if (sDisplayStyle.equals(DisplayStyle.VECTORFIELD)) {

                                createVectorFieldFilter(defaultLookupTable, ug,
                                        startsWith, elementInFile, index, fieldScaleFactor,
                                        sDisplayStyle, visualization);
                            }

                            if (bShowOutline) {

                                createOutlineFilter(ug, visualization);
                            }

                            VSwingUtil.invokeAndWait(new Runnable() {

                                public void run() {

                                    VTKOutputType vtkOutput = ((VTKOutputType) (((MultipleOutputType) mRep.getReturnValue()).getTypeContainers().get(0).getTypeRepresentation()));
                                    vtkOutput.emptyView();
                                    vtkOutput.setViewValue(visualization);

                                    String[] split = fileName.split(".vtu");
                                    if (makePNG) {
                                        File pngFile = new File(split[0] + ".png");
                                        vtkOutput.saveImage(pngFile);
                                    }
                                }
                            });

                            lastVisualization = visualization;

                            try {
                                System.out.println("WAIT IN FILELOOP BEGIN");
                                Thread.sleep(wait);
                                System.out.println("WAIT IN FILELOOP END");
                            } catch (InterruptedException ex) {
                                Logger.getLogger(VTUViewer.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } // end for files

                        lastAllFiles = allFiles;
                        System.out.println("Setting last files");

                        // if plotting only a single file, exit here
                        if (fileOrFolder != null && !fileOrFolder.isDirectory()) {
                            System.out.println("IS Directory, setting running false");
                            //running = false;
                            break;
                        }

                        System.out.println("IN WHILE LOOP AT END");
                    } // end while
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }

                System.out.println(" LEAVING WHILE LOOP");
                setThread(null);
                lastAllFiles = new ArrayList<File>();
            }
        }));

        startThread();

        File outFile;
        if (fileOrFolder.isDirectory()) {
            outFile = new File(fileOrFolder, startsWith + ".vtu");

        } else {
            outFile = fileOrFolder;
        }

        //return lastVisualization;
        return new Object[]{lastVisualization, outFile};
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

        double[] valRange;

        if (sRange.equals(Range.AUTO)) {

            valRange = getRange(sDisplayStyle, ug, elementInFile);

            minValueRange = valRange[0];
            maxValueRange = valRange[1];
        }

        defaultLookupTable.SetTableRange(minValueRange, maxValueRange);
        defaultLookupTable.SetHueRange(0.0, 1);
        defaultLookupTable.SetSaturationRange(0.6, 1);
        defaultLookupTable.SetValueRange(1, 1);

        defaultLookupTable.Build();

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

        if (sDataStyle.equals(DataStyle.WARP_FACTOR)) {
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
            String elementInFile, int index, double scaleFactor,
            String sDisplayStyle, final Visualization visualization) {

        vtkLookupTable vectorfieldTable = new vtkLookupTable();
        vectorfieldTable.DeepCopy(defaultLookupTable);

        // represent vector field
        vtkArrowSource arrowSource = new vtkArrowSource();

        vtkGlyph3D vectorGlyph = new vtkGlyph3D();
        vectorGlyph.SetInput(ug);
        vectorGlyph.SetSourceConnection(arrowSource.GetOutputPort());
        vectorGlyph.SetScaleModeToScaleByVector();
        vectorGlyph.SetVectorModeToUseVector();
        vectorGlyph.ScalingOn();
        vectorGlyph.OrientOn();
        vectorGlyph.SetColorModeToColorByVector();//color the glyphs
        vectorGlyph.SetScaleFactor(scaleFactor);
        vectorGlyph.SetInputArrayToProcess(index, ug.GetInformation());
        vectorGlyph.Update();


        vtkPolyDataMapper vectorGlyphMapper = new vtkPolyDataMapper();
        vectorGlyphMapper.SetInput(vectorGlyph.GetOutput());

        settingsForMappers(sDisplayStyle, vectorGlyphMapper, vectorfieldTable);

        //optional setttings for lightning
        vtkProperty sliceProp = new vtkProperty();
        sliceProp.SetDiffuse(0.0);
        sliceProp.SetSpecular(0.0);
        sliceProp.SetAmbient(1.0);

        vtkActor vectorActor = new vtkActor();
        vectorActor.SetMapper(vectorGlyphMapper);
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
     * @param sDisplayStyle
     * @param ug
     * @param elementInFile
     * 
     * @returns true if component could be set
     */
    private boolean settingScalarsOrVectors(String sDisplayStyle,
            vtkUnstructuredGrid ug,
            String elementInFile) {

        vtkDataArray id = ug.GetPointData().GetArray(elementInFile);

        if (id == null) {
            return false;
        }

        if (sDisplayStyle.equals(DisplayStyle.VECTORFIELD)) {

            ug.GetPointData().SetVectors(id);
//            ug.GetPointData().SetActiveVectors(elementInFile);

        } else {

            ug.GetPointData().SetScalars(id);
//            ug.GetPointData().SetActiveScalars(elementInFile);
        }

        return true;
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

        if (sDisplayStyle.equals(DisplayStyle.VECTORFIELD)) {

            dataArray = ug.GetPointData().GetVectors();


        } else {

            dataArray = ug.GetPointData().GetScalars();
        }

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
//        mapper.SetInput(filter.GetOutput()); // mapper has no SetInput()

        mapper.SetLookupTable(lookupTable);

        if (!sDisplayStyle.equals(DisplayStyle.VECTORFIELD)) {

            mapper.SetScalarRange(lookupTable.GetTableRange());
            mapper.ScalarVisibilityOn();
            mapper.SetColorModeToMapScalars();

        } else if (sDisplayStyle.equals(DisplayStyle.VECTORFIELD)) {

            mapper.SetScalarRange(lookupTable.GetTableRange());

//            mapper.ScalarVisibilityOff();
            mapper.ScalarVisibilityOn();//color for glyphs


        }

        mapper.Update();
    }

    private void setDisplayStyle(vtkActor actor, String style) {

        if (style.equals(DisplayStyle.SURFACE)) {
            actor.GetProperty().SetRepresentationToSurface();
        } else if (style.equals(DisplayStyle.SURFACE_EDGE)) {
            actor.GetProperty().SetRepresentationToSurface();
            actor.GetProperty().EdgeVisibilityOn();
        } else if (style.equals(DisplayStyle.WIREFRAME)) {
            actor.GetProperty().SetRepresentationToWireframe();
            actor.GetProperty().SetAmbient(1.0);
            actor.GetProperty().SetDiffuse(0.0);
            actor.GetProperty().SetSpecular(0.0);
        } else if (style.equals(DisplayStyle.POINTS)) {
            actor.GetProperty().SetRepresentationToPoints();
            actor.GetProperty().SetPointSize(2.5);
        } else if (style.equals(DisplayStyle.VECTORFIELD)) {
//            actor.GetProperty().SetRepresentationToPoints();
//            actor.ApplyProperties();                    
        } else {
            throw new RuntimeException("Style not found.");
        }
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

                    boolean nameAccept = startsWith.equals("") || fileName.startsWith(startsWith + "_");

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

        return result;
    }

    @MethodInfo(noGUI = true, callOptions = "assign-window")
    public void setParameterVisibility(VisualObject vObj) {

        VisualCanvas canvas = (VisualCanvas) vObj.getMainCanvas();

        final DefaultMethodRepresentation mRep = vObj.getObjectRepresentation().
                getMethodBySignature("visualize", MethodRequest.class,
                File.class, String.class, String.class,
                boolean.class, String.class, long.class,
                String.class, double.class, double.class,
                boolean.class, boolean.class, boolean.class,
                String.class, String.class, double.class,
                int.class, double.class);

        System.out.println(" - - " + getClass().getSimpleName() + ".showCode(): ");

        // START setting some parameters visible or not, depending on other parameters

//        Assume mReq is parameter 0

        final ActionListener fileOrFolderListener = new ActionListener() {
            // set visibility for startsWith (2)
            // depending on value of parameter folder (1)

            public void actionPerformed(ActionEvent e) {

                System.out.println(" - - before fileOrFolderListener: ");

                if (e.getActionCommand().equals(
                        LoadObserveFileType.FILE_OR_FOLDER_LOADED_ACTION)) {

                    File fileOrFolder = (File) mRep.getParameter(1).getViewValueWithoutValidation();

                    // if folder does not exist, do nothing
                    if (fileOrFolder == null) {
                        return;
                    }

//                    LoadObserveFileType tRep1 = (LoadObserveFileType) mRep.getParameter(1);
//                    File folder = tRep1.getFileManager().getLatestFileOrFolder();

                    System.out.println(" - - fileOrFolderListener: " + fileOrFolder.getName());

                    TypeRepresentationBase tRep = mRep.getParameter(2);

                    if (fileOrFolder.isDirectory()) {

                        System.out.println(" - - folder.isDirectory() = " + fileOrFolder.isDirectory());

                        tRep.setVisible(true);
                        tRep.getConnector().setVisible(true);
                        mRep.getParameter(1).setValueName("Folder");

                    } else if (fileOrFolder.isFile()) {

                        System.out.println(" - - folder.isFile() = " + fileOrFolder.isFile());

                        tRep.setVisible(false);
                        tRep.getConnector().setVisible(false);
                        mRep.getParameter(1).setValueName("File");
                    }

                }
            }
        };

        final ActionListener sRangeListener = new ActionListener() {
            // set visibility for min- max-ValueRange (8 & 9)
            // depending on value of parameter sRange (7)

            public void actionPerformed(ActionEvent e) {

                System.out.println(" - - before sRangeListener: ");

                if (e.getActionCommand().equals(
                        //                        TypeRepresentationBase.SET_VIEW_VALUE_ACTION
                        SelectionInputType.SELECTION_CHANGED_ACTION)) {

                    String sRange = (String) mRep.getParameter(7).getViewValueWithoutValidation();

                    System.out.println(" - - sRangeListener: " + sRange);

                    TypeRepresentationBase tRep8 = mRep.getParameter(8);
                    TypeRepresentationBase tRep9 = mRep.getParameter(9);

                    if (sRange.equals(Range.AUTO)) {

                        tRep8.setVisible(false);
                        tRep8.getConnector().setVisible(false);

                        tRep9.setVisible(false);
                        tRep9.getConnector().setVisible(false);

                    } else if (sRange.equals(Range.MIN_MAX)) {

                        tRep8.setVisible(true);
                        tRep8.getConnector().setVisible(true);

                        tRep9.setVisible(true);
                        tRep9.getConnector().setVisible(true);
                    }
                }
            }
        };

        final ActionListener warpListener = new ActionListener() {
            // set visibility for warpfactor (15)
            // depending on value of parameter sDataStyle (14)

            public void actionPerformed(ActionEvent e) {


                System.out.println(" - - before warpListener: ");

                if (e.getActionCommand().equals(
                        //                        TypeRepresentationBase.SET_VIEW_VALUE_ACTION
                        SelectionInputType.SELECTION_CHANGED_ACTION)) {

                    String sDataStyle = (String) mRep.getParameter(14).getViewValueWithoutValidation();

                    System.out.println(" - - warpListener: " + sDataStyle);

                    TypeRepresentationBase tRep = mRep.getParameter(15);

                    if (sDataStyle.equals(DataStyle.WARP_FACTOR)) {

                        tRep.setVisible(true);
                        tRep.getConnector().setVisible(true);

                    } else {

                        tRep.setVisible(false);
                        tRep.getConnector().setVisible(false);
                    }

                }
            }
        };

        final ActionListener contourListener = new ActionListener() {
            // set visibility for contourfactor (16)
            // depending on value of parameter sDataStyle (14)

            public void actionPerformed(ActionEvent e) {

                System.out.println(" - - before contourListener: ");

                if (e.getActionCommand().equals(
                        //                        TypeRepresentationBase.SET_VIEW_VALUE_ACTION
                        SelectionInputType.SELECTION_CHANGED_ACTION)) {

                    String sDataStyle = (String) mRep.getParameter(14).getViewValueWithoutValidation();

                    System.out.println(" - - contourListener: " + sDataStyle);

                    TypeRepresentationBase tRep = mRep.getParameter(16);

                    if (sDataStyle.equals(DataStyle.CONTOUR)) {
                        tRep.setVisible(true);
                        tRep.getConnector().setVisible(true);

                    } else {
                        tRep.setVisible(false);
                        tRep.getConnector().setVisible(false);
                    }
                }
            }
        };

        final ActionListener fieldScaleListener = new ActionListener() {
            // set visibility for contourfactor (17)
            // depending on value of parameter sDisplayStyle (13)

            public void actionPerformed(ActionEvent e) {

                System.out.println(" - - before fieldScaleListener: ");

                if (e.getActionCommand().equals(
                        //                        TypeRepresentationBase.SET_VIEW_VALUE_ACTION
                        SelectionInputType.SELECTION_CHANGED_ACTION)) {

                    String sDisplayStyle = (String) mRep.getParameter(13).getViewValueWithoutValidation();

                    // START for DEBUG only
                    String sDataStyle = (String) mRep.getParameter(14).getViewValueWithoutValidation();
                    System.out.println(" - - fieldScaleListener: Styles: Display = "
                            + sDisplayStyle + " , Data = " + sDataStyle);
                    // END for DEBUG only

                    TypeRepresentationBase tRep = mRep.getParameter(17);

                    if (sDisplayStyle.equals(DisplayStyle.VECTORFIELD)) {

                        tRep.setVisible(true);
                        tRep.getConnector().setVisible(true);

                    } else {

                        tRep.setVisible(false);
                        tRep.getConnector().setVisible(false);

                    }
                }
            }
        };

//        System.out.println(" - - mRep.getParameter(13) = " + mRep.getParameter(13));

        mRep.getParameter(13).getActionListeners().add(fieldScaleListener);
        mRep.getParameter(14).getActionListeners().add(contourListener);
        mRep.getParameter(14).getActionListeners().add(warpListener);
        mRep.getParameter(7).getActionListeners().add(sRangeListener);
        mRep.getParameter(1).getActionListeners().add(fileOrFolderListener);


        // START init action (dirty but needed)
        ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
        listeners.add(fieldScaleListener);
        listeners.add(contourListener);
        listeners.add(warpListener);
        listeners.add(sRangeListener);
        listeners.add(fileOrFolderListener);

        System.out.println(" - - fore ActionListeners.actionPerformed(): ");
        for (ActionListener al : listeners) {
            al.actionPerformed(new ActionEvent(this, 0, TypeRepresentationBase.SET_VIEW_VALUE_ACTION));
            al.actionPerformed(new ActionEvent(this, 0, LoadObserveFileType.FILE_OR_FOLDER_LOADED_ACTION));
            al.actionPerformed(new ActionEvent(this, 0, SelectionInputType.SELECTION_CHANGED_ACTION));
        }
        // END init action (dirty but needed)

        System.out.println(" - -  ActionListener added ");

        // END setting some parameters visable or not depending on others


    }
}
