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
@ComponentInfo(name = "VTUViewer_Glyph", category = "Custom")
public class VTUViewer_Glyph implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private transient DefaultMethodRepresentation mRep;

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
//        }else if (style == "Vectorfield") {
////            actor.GetProperty().Set
//            String msg = " --- setDisplayStyle( .. , Vectorfield) nothing done.";
//            System.out.println(msg);
//            VMessage.info("debug", msg);
//            
        } else {
            System.out.println("Style [" + style + "] not found or nothing to do.");
        }
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

    public Visualization visualizeAll(
            MethodRequest mReq,
            @ParamGroupInfo(group = "Files|false|File depending data.")
            @ParamInfo(name = "folderWithPlotFiles", style = "load-folder-dialog") File folder,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "beginning, e.g. \"file00\"") String startsWith,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "elementInFile", options = "value=0") int elementInFile,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "makePNG", options = "value=false")
            final boolean makePNG,
            @ParamGroupInfo(group = "Plot|false|Plot depending data.")
            @ParamInfo(name = "Title", style = "default") String title,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "waiting in ms", options = "value=0")
            final long wait,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Range", style = "selection", options = "value=[\"Auto\",\"Min/Max\"]") String sRange,
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


        ArrayList<File> allFiles = getAllFilesInFolder(folder, startsWith, "vtu");

        System.out.println("-- There are " + allFiles.size() + " files in the folder.");

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

            System.out.println("ELEMENTS/ARRAYS IN FILE:");
            for (int i = 0; i < numVisCompData; i++) {

                System.out.println(i + ") " + ug.GetPointData().GetArrayName(i));

            }


            System.out.println("Components IN FILE:");
            for (int i = 0; i < ug.GetPointData().GetNumberOfComponents(); i++) {

                System.out.println("(" + i + ",0) " + ug.GetPointData().GetComponent(i, 0));

            }

            System.out.println("Tuples IN FILE: " + ug.GetPointData().GetNumberOfTuples());


            if (numVisCompData < elementInFile) {
                String msg = "There are only " + numVisCompData + " elements in the selected file."
                        + " And you want to select: " + elementInFile;

                VMessage.error("Wrong Parameter", msg);

                System.err.println(msg);

                VMessage.info("Hint", "Notice that if you want to get the first element"
                        + "in file you need to tip 0 (zero).");

                return visualization;
            }

            String visCompDataName = ug.GetPointData().GetArrayName(elementInFile);

            VMessage.info("visCompDataName is " + elementInFile + "elementInFile", visCompDataName);

            ug.GetPointData().SetScalars(ug.GetPointData().GetArray(visCompDataName));

//            ////////////////////////////////////
//            // create value lookup table
//            ////////////////////////////////////
//            vtkLookupTable hueLut = new vtkLookupTable();
//
//            if (sRange == "Auto") {
//                double[] valRange = ug.GetPointData().GetScalars().GetRange();
//                minValueRange = valRange[0];
//                maxValueRange = valRange[1];
//            }
//            hueLut.SetTableRange(minValueRange, maxValueRange);
//            hueLut.SetHueRange(0.0, 1);
//            hueLut.SetSaturationRange(0.6, 1);
//            hueLut.SetValueRange(1, 1);
//            hueLut.Build();
//
//            if (bShowLegend) {
//                visualization.setLookupTable(hueLut);
//            }

//            ////////////////////////////////////
//            // create plain data visualization
//            ////////////////////////////////////
//            if (sDataStyle == "None") {
//                vtkLookupTable plainMapperTable = new vtkLookupTable();
//                plainMapperTable.DeepCopy(hueLut);
//
//                vtkDataSetMapper plainMapper = new vtkDataSetMapper();
//                plainMapper.SetInput(ug);
//                plainMapper.ScalarVisibilityOn();
//                plainMapper.SetColorModeToMapScalars();
//                plainMapper.SetScalarRange(plainMapperTable.GetTableRange());
//                plainMapper.SetLookupTable(plainMapperTable);
//
//                vtkActor plainActor = new vtkActor();
//                plainActor.SetMapper(plainMapper);
//                setDisplayStyle(plainActor, sDisplayStyle);
//
//                visualization.addActor(plainActor);
//            }
//
//            ////////////////////////////////////
//            // create warped data visualization
//            ////////////////////////////////////
//            if (sDataStyle == "Warp (Auto)" || sDataStyle == "Warp (Factor)") {
//                double[] valueMinMax = ug.GetPointData().GetScalars().GetRange();
//                double factor = 1.0 / (valueMinMax[1] - valueMinMax[0]);
//                if (sDataStyle == "Warp (Factor)") {
//                    factor = warpFactor;
//                }
//
//                vtkWarpScalar warpScalar = new vtkWarpScalar();
//                warpScalar.SetInput(ug);
//                warpScalar.SetScaleFactor(factor);
//                warpScalar.Update();
//
//                vtkLookupTable warpTable = new vtkLookupTable();
//                warpTable.DeepCopy(hueLut);
//
//                vtkDataSetMapper warpMapper = new vtkDataSetMapper();
//                warpMapper.SetInputConnection(warpScalar.GetOutputPort());
//                warpMapper.SetScalarRange(warpTable.GetTableRange());
//                warpMapper.SetLookupTable(warpTable);
//
//                vtkActor warpActor = new vtkActor();
//                warpActor.SetMapper(warpMapper);
//                setDisplayStyle(warpActor, sDisplayStyle);
//
//                visualization.addActor(warpActor);
//            }
//
//            ////////////////////////////////////
//            // create contour filter
//            ////////////////////////////////////
//            if (sDataStyle == "Contour") {
//
//                vtkLookupTable contTable = new vtkLookupTable();
//                contTable.DeepCopy(hueLut);
//
//                vtkContourFilter contours = new vtkContourFilter();
//                contours.SetInput(ug);
//                contours.GenerateValues(numContours, contTable.GetTableRange());
//
//                vtkPolyDataMapper contMapper = new vtkPolyDataMapper();
//                contMapper.SetInput(contours.GetOutput());
//                contMapper.SetScalarRange(contTable.GetTableRange());
//                contMapper.SetLookupTable(contTable);
//
//                vtkActor contActor = new vtkActor();
//                contActor.SetMapper(contMapper);
//                setDisplayStyle(contActor, sDisplayStyle);
//
//                visualization.addActor(contActor);
//            }

            ////////////////////////////////////
            // create vector field
            ////////////////////////////////////
            if (sDisplayStyle.equals("Vectorfield")) {


                reader.SetFileName(file.getAbsolutePath());
                reader.Update();
                vtkUnstructuredGrid image = reader.GetOutput();
                image.GetPointData().SetVectors(image.GetPointData().GetArray(elementInFile));


                // represent vector field
                vtkGlyph3D vectorGlyph = new vtkGlyph3D();
                vtkArrowSource arrowSource = new vtkArrowSource();
                vtkPolyDataMapper vectorGlyphMapper = new vtkPolyDataMapper();

                int n = image.GetPointData().GetNumberOfArrays();
                for (int i = 0; i < n; i++) {
                    System.out.println("name of array[" + i + "]: " + image.GetPointData().GetArrayName(i));
                }


                vectorGlyph.SetInputConnection(image.GetProducerPort());

                vectorGlyph.SetSourceConnection(arrowSource.GetOutputPort());
                vectorGlyph.SetScaleModeToScaleByVector();
                vectorGlyph.SetVectorModeToUseVector();
                vectorGlyph.ScalingOn();
                vectorGlyph.OrientOn();
                vectorGlyph.SetInputArrayToProcess(
                        elementInFile,
                        image.GetInformation());

                vectorGlyph.SetScaleFactor(scaleFactor);

                vectorGlyph.Update();

                vectorGlyphMapper.SetInputConnection(vectorGlyph.GetOutputPort());
                vectorGlyphMapper.Update();

                vtkActor vectorActor = new vtkActor();
                vectorActor.SetMapper(vectorGlyphMapper);


                visualization.addActor(vectorActor);
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

                        String[] split = fileName.split(".vtu");
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
