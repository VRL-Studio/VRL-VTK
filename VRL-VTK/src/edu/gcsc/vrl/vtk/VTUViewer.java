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
import eu.mihosoft.vrl.visual.VSwingUtil;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import vtk.vtkActor;
import vtk.vtkArrowSource;
import vtk.vtkContourFilter;
import vtk.vtkDataSetMapper;
import vtk.vtkGlyph2D;
import vtk.vtkGlyph3D;
import vtk.vtkLookupTable;
import vtk.vtkOutlineFilter;
import vtk.vtkPolyDataMapper;
import vtk.vtkUnstructuredGrid;
import vtk.vtkWarpScalar;
import vtk.vtkXMLUnstructuredGridReader;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 * @author Andreas Vogel <andreas.vogel@gcsc.uni-frankfurt.de>
 * @author Christian Poliwoda <christian.poliwoda@gcsc.uni-frankfurt.de>
 */
@ComponentInfo(name = "VTUViewer", category = "Custom")
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
        if (sRange == "Auto") {
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
        if (sDataStyle == "None") {
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
        if (sDataStyle == "Warp (Auto)" || sDataStyle == "Warp (Factor)") {
            double[] valueMinMax = ug.GetPointData().GetScalars().GetRange();
            double factor = 1.0 / (valueMinMax[1] - valueMinMax[0]);
            if (sDataStyle == "Warp (Factor)") {
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
        if (sDataStyle == "Contour") {

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
        if (style == "Surface") {
            actor.GetProperty().SetRepresentationToSurface();
        } else if (style == "Surface/Edge") {
            actor.GetProperty().SetRepresentationToSurface();
            actor.GetProperty().EdgeVisibilityOn();
        } else if (style == "Wireframe") {
            actor.GetProperty().SetRepresentationToWireframe();
            actor.GetProperty().SetAmbient(1.0);
            actor.GetProperty().SetDiffuse(0.0);
            actor.GetProperty().SetSpecular(0.0);
        } else if (style == "Points") {
            actor.GetProperty().SetRepresentationToPoints();
            actor.GetProperty().SetPointSize(2.5);
        }else if (style == "Vectorfield") {
//            actor.GetProperty().Set
            String msg = " --- setDisplayStyle( .. , Vectorfield) nothing done.";
            System.out.println(msg);
            VMessage.info("debug", msg);
            
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

                    boolean nameAccept = startsWith == "" || fileName.startsWith(startsWith);

                    return fileAccept && nameAccept;
                }
            })) {
                if (f.isFile()) {
                    result.add(f);
                }
            }

        } else {
            //
            throw new RuntimeException("Viewer: path '" + dir.getName() + "' not found.");
        }

        return result;
    }

//    @MethodInfo( valueStyle="default")
    public Visualization visualizeAll(
            MethodRequest mReq,
            @ParamGroupInfo(group = "Files|false|File depending data.")
            @ParamInfo(name = "folderWithPlotFiles", style = "load-folder-dialog")
                    File folder,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "beginning, e.g. \"file00\"") 
                    String startsWith,
//            @ParamGroupInfo(group = "Files")
//            @ParamInfo(name = "ending, e.g. \"vtu\"")
//            final String ending,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "elementInFile", options = "value=0")
                    int elementInFile,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "makePNG", options = "value=false")
            final boolean makePNG,
            @ParamGroupInfo(group = "Plot|false|Plot depending data.")
            @ParamInfo(name = "Title", style = "default") 
                    String title,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "waiting in ms", options = "value=0")
            final long wait,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Range", style = "selection", options = "value=[\"Auto\",\"Min/Max\"]") 
                    String sRange,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Min", style = "default", options = "value=0") 
                    double minValueRange,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Max", style = "default", options = "value=1") 
                    double maxValueRange,
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
            @ParamInfo(name = "Show Data Legend", style = "default", options = "value=true") 
                    boolean bShowLegend,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Show Outline", style = "default", options = "value=false")
                    boolean bShowOutline,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Show Orientation", style = "default")
                    boolean showOrientation,
            @ParamGroupInfo(group = "Filters|false|Choose which filter should be used.")
            @ParamInfo(name = "Display Style", style = "selection", 
                    options = "value=[\"Surface\",\"Surface/Edge\",\"Wireframe\",\"Points\", \"Vectorfield\"]") 
                    String sDisplayStyle,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Data Filter", style = "selection", 
                    options = "value=[\"None\",\"Warp (Auto)\",\"Warp (Factor)\", \"Contour\"]")
                    String sDataStyle,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Warp Factor", style = "default", options = "value=1") 
                    double warpFactor,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Num Contour", style = "default", options = "value=5") 
                    int numContours) {

        mRep = mReq.getMethod();


//        // read input
//        if (bUseInternFilename) {
//            folder = InternFile;
//        }
//
//        if (folder == null) {
//            return new Visualization();
//        }
//        if (!folder.exists()) {
//            return new Visualization();
//        }


        ArrayList<File> allFiles = getAllFilesInFolder(folder, startsWith, "vtu");

        System.out.println("-- allFiles.size() = " + allFiles.size());

        Visualization lastVisualization = null;

        for (File file : allFiles) {

            // create vis object to add all components
            final Visualization visualization = new Visualization();


            final String fileName = file.getAbsolutePath();//getAbsoluteFile();
//            System.out.println("-- fileName = "+ fileName);


            if (title.isEmpty()) {

                visualization.setTitle(file.getName());
            } else {
                visualization.setTitle(title);
            }



            vtkXMLUnstructuredGridReader reader = new vtkXMLUnstructuredGridReader();

//            System.out.println("-- reader = "+ reader);
            reader.SetFileName(fileName);
            reader.Update();
            vtkUnstructuredGrid ug = reader.GetOutput();

            ////////////////////////////////////
            // get point Data for component
            ////////////////////////////////////
            int numVisCompData = ug.GetPointData().GetNumberOfArrays();
            
//            if (numVisCompData != 1) {
//                return visualization;
//            }
//            throw new RuntimeException("Too many/few components: "+numVisCompData);
            
            if (numVisCompData < elementInFile) {
                String msg = "There are only "+ numVisCompData +" elements in the selected file."
                        + " And you want to select: "+ elementInFile;
                
                VMessage.error("Wrong Parameter", msg);
                
                System.err.println(msg);
                
                VMessage.info("Hint", "Notice that if you want to get the first element"
                        + "in file you need to tip 0 (zero).");
                
                return visualization;
            }
            
            String visCompDataName = ug.GetPointData().GetArrayName(elementInFile);
            
            VMessage.info("visCompDataName is "+elementInFile+ "elementInFile", visCompDataName);
            
            ug.GetPointData().SetScalars(ug.GetPointData().GetArray(visCompDataName));

            ////////////////////////////////////
            // create value lookup table
            ////////////////////////////////////
            vtkLookupTable hueLut = new vtkLookupTable();
            
            if (sRange == "Auto") {
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
            if (sDataStyle == "None") {
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
            if (sDataStyle == "Warp (Auto)" || sDataStyle == "Warp (Factor)") {
                double[] valueMinMax = ug.GetPointData().GetScalars().GetRange();
                double factor = 1.0 / (valueMinMax[1] - valueMinMax[0]);
                if (sDataStyle == "Warp (Factor)") {
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
            if (sDataStyle == "Contour") {

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
            // create vector field
            ////////////////////////////////////
            if (sDataStyle == "Vectorfield") {
                
                vtkArrowSource arrow = new vtkArrowSource();
                arrow.SetTipResolution(6);
                arrow.SetTipRadius(0.1);
                arrow.SetTipLength(0.35);
                arrow.SetShaftResolution(6);
                arrow.SetShaftRadius( 0.03);

//                vtkGlyph3D glyph = new vtkGlyph3D();
                vtkGlyph2D glyph = new vtkGlyph2D();
                glyph.SetInput(ug);//reader.GetOutput(elementInFile));//reader.GetOutputPort());
                glyph.SetSource(arrow.GetOutput());//arrow.GetOutputPort());
                glyph.SetVectorModeToUseVector();
                glyph.SetColorModeToColorByScalar();
                glyph.SetScaleModeToDataScalingOff();
                glyph.OrientOn();
                glyph.SetScaleFactor(0.2);

                vtkPolyDataMapper glyphMapper = new vtkPolyDataMapper();
                glyphMapper.SetInput(glyph.GetOutput());
                glyphMapper.SetLookupTable(hueLut);
                glyphMapper.ScalarVisibilityOn();
                //   eval
                glyphMapper.SetScalarRange(reader.GetOutput().GetScalarRange());

                vtkActor glyphActor = new vtkActor();
                glyphActor.SetMapper( glyphMapper);//contourMapper);

                visualization.addActor(glyphActor);
                
////                // Setup the arrows
////                vtkArrowSource arrowSource = new vtkArrowSource();
////                arrowSource.SetInputConnection(elementInFile, ug.GetProducerPort());
////                arrowSource.Update();
////
////                vtkGlyph2D glyph = new vtkGlyph2D();
////                glyph.SetInput(elementInFile,ug);
////                glyph.SetSourceConnection(arrowSource.GetOutputPort());
////                glyph.SetVectorModeToUseNormal();
////                glyph.SetScaleModeToScaleByVector();
////                glyph.SetScaleFactor(0.25);
////                
////
////                vtkPolyDataMapper spikeMapper = new vtkPolyDataMapper();
////                spikeMapper.SetInputConnection(glyph.GetOutputPort());
////                spikeMapper.SetLookupTable(hueLut);
////                
////                vtkActor spikeActor = new vtkActor();
////                spikeActor.SetMapper(spikeMapper);
////
////                
////                setDisplayStyle(spikeActor, sDisplayStyle);
////
////                visualization.addActor(spikeActor);
                
//                vtkGlyph3D glyph = new vtkGlyph3D();
//                glyph.SetInputConnection(sphere.GetOutputPort());
//                glyph.SetSourceConnection(cone.GetOutputPort());
//                glyph.SetVectorModeToUseNormal();
//                glyph.SetScaleModeToScaleByVector();
//                glyph.SetScaleFactor(0.25);
//
//                vtkPolyDataMapper spikeMapper = new vtkPolyDataMapper();
//                spikeMapper.SetInputConnection(glyph.GetOutputPort());
//                
//                vtkActor spikeActor = new vtkActor();
//                spikeActor.SetMapper(spikeMapper);
//
//                visualization.addActor(spikeActor);

//  vtkSmartPointer<vtkArrowSource> arrowSource = new vtkSmartPointer(vtkArrowSource);
//  arrowSource.Update();
// 
//  vtkSmartPointer<vtkGlyph2D> glyphFilter = new vtkSmartPointer<vtkGlyph2D>();
//  glyphFilter.SetSourceConnection(arrowSource.GetOutputPort());
//  glyphFilter.OrientOn();
//  glyphFilter.SetVectorModeToUseVector();
//
//  glyphFilter.SetInputData(image);
//
//  glyphFilter.Update();
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



            VSwingUtil.invokeAndWait(new Runnable() {
                public void run() {
                    mRep.getReturnValue().emptyView();
                    mRep.getReturnValue().setViewValue(visualization);

                    if (makePNG) {

                        String[] split = fileName.split(".vtu" );
                        File pngFile = new File(split[0] + ".png");
                        ((VTKOutputType) mRep.getReturnValue()).saveImage(pngFile);
                    }

                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException ex) {
//                     Logger.getLogger(VTUViewer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            lastVisualization = visualization;

        }//for (File file : allFiles)

        return lastVisualization;
    }
}
