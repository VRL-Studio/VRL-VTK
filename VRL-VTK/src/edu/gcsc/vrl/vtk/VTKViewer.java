/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.annotation.ComponentInfo;
import eu.mihosoft.vrl.annotation.MethodInfo;
import eu.mihosoft.vrl.annotation.ParamGroupInfo;
import eu.mihosoft.vrl.annotation.ParamInfo;
import eu.mihosoft.vrl.reflection.DefaultMethodRepresentation;
import eu.mihosoft.vrl.types.MethodRequest;
import java.io.File;
import vtk.*;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 * @author Andreas Vogel <andreas.vogel@gcsc.uni-frankfurt.de>
 */
@ComponentInfo(name = "VTK-Viewer", category = "VTK")
public class VTKViewer implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private boolean bUseInternFilename = false;
    private File InternFile;
    private boolean running = false;
    private transient DefaultMethodRepresentation mRep;

    // add your code here
    // @MethodInfo(hide=false, callOptions="autoinvoke")
    public Visualization visualize(
            MethodRequest mReq,
            @ParamGroupInfo(group = "Data|true|Select Input Data")
            @ParamInfo(name = "Filename",
            nullIsValid = true,
            style = "load-dialog",
            options = "endings=[\"vtu\"]; invokeOnChange=false") java.io.File file,
            @ParamGroupInfo(group = "Data")
            @ParamInfo(name = "Title", style = "default", options = "invokeOnChange=true") String title,
            @ParamGroupInfo(group = "Data")
            @ParamInfo(name = "Range", style = "selection", options = "value=[\"Auto\",\"Min/Max\"];invokeOnChange=true") String sRange,
            @ParamGroupInfo(group = "Data")
            @ParamInfo(name = "Min", style = "default", options = "value=0;invokeOnChange=true") double minValueRange,
            @ParamGroupInfo(group = "Data")
            @ParamInfo(name = "Max", style = "default", options = "value=1;invokeOnChange=true") double maxValueRange,
            @ParamGroupInfo(group = "Outline & Orientation|false|Define whether to show Outline, Orientation and Data Legend")
            @ParamInfo(name = "Show Data Legend", style = "default", options = "value=true;invokeOnChange=true") boolean bShowLegend,
            @ParamGroupInfo(group = "Outline & Orientation")
            @ParamInfo(name = "Show Outline", style = "default", options = "value=false;invokeOnChange=true") boolean bShowOutline,
            @ParamGroupInfo(group = "Outline & Orientation")
            @ParamInfo(name = "Show Orientation", style = "default", options = "invokeOnChange=true") boolean showOrientation,
            @ParamGroupInfo(group = "Display Style and Filter|false|Select Display Style and Filter")
            @ParamInfo(name = "Display Style", style = "selection", options = "value=[\"Surface\",\"Surface/Edge\",\"Wireframe\",\"Points\"];invokeOnChange=true") String sDisplayStyle,
            @ParamGroupInfo(group = "Display Style and Filter")
            @ParamInfo(name = "Data Filter", style = "selection", options = "value=[\"None\",\"Warp (Auto)\",\"Warp (Factor)\", \"Contour\"];invokeOnChange=true") String sDataStyle,
            @ParamGroupInfo(group = "Display Style and Filter")
            @ParamInfo(name = "Warp Factor", style = "default", options = "value=1;invokeOnChange=true") double warpFactor,
            @ParamGroupInfo(group = "Display Style and Filter")
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

        String fileName = file.getAbsolutePath();
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
        if ("Auto".equals(sRange)) {
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
        if ("None".equals(sDataStyle)) {
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
        if ("Warp (Auto)".equals(sDataStyle) || "Warp (Factor)".equals(sDataStyle)) {
            double[] valueMinMax = ug.GetPointData().GetScalars().GetRange();
            double factor = 1.0 / (valueMinMax[1] - valueMinMax[0]);
            if ("Warp (Factor)".equals(sDataStyle)) {
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
        if ("Contour".equals(sDataStyle)) {

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
        } else {
            throw new RuntimeException("Style not found.");
        }
    }

//    @MethodInfo(valueName="VTU-File", valueStyle="silent")
//    public java.io.File start(
//        CanvasRequest cReq,
//        VisualIDRequest idReq,
//        @ParamInfo(style="save-folder-dialog") File dir,
//        @ParamInfo(name="filename begin", options="value=\"data\"") String startsWith) {
//        
//        String ending = "vtu";
//        
//        VisualCanvas mainCanvas = cReq.getCanvas();
//       
//        dir.eachFileRecurse() {f->
//              if (f.getName().endsWith(".vtu")) {
//                f.delete()
//              }
//        }
//         
//        running = true
//        bUseInternFilename = true;
//        
//        Thread.start {
//            File lastFile = null
//            while(running) {
//                try {
//                def f = getLastFileInFolder(dir, startsWith,ending)
//                
//                if (!f.equals(lastFile)) {
//                    InternFile = f;
//                    
//                    /*mainCanvas.getInspector().invokeFromInvokeButton(
//                        mRep.getDescription(),
//                        idReq.getID()
//                    );*/
//
//                    mRep.invokeAsCallParent(false);
//                } 
//                lastFile = f
//                
//                } catch (Exception ex) {
//                }
//
//                sleep(1000)
//            }
//        }
//
//        String tmp = dir.getAbsolutePath()+ "/" + startsWith + ".vtu";
//        tmp.replace("//", "/");
//    return new File(tmp);
//    }
    @MethodInfo()
    public void stop() {
        running = false;
        bUseInternFilename = false;
    }
//    private File getLastFileInFolder (@ParamInfo(style="load-folder-dialog") File dir,
//       @ParamInfo(name="beginning, e.g. \"file00\"") String startsWith,
//       @ParamInfo(name="ending, e.g. \"vtu\"") String ending) {
//       
//       def result = new ArrayList<File>()
//       
//        if (dir != null && dir.isDirectory()) {
//            for (File f : dir.listFiles(new FileFilter() {
//                @Override
//                public boolean accept(File pathName) {
//                     def fileAccept = pathName.getName().toLowerCase().endsWith("."+ending) ||
//                     pathName.isDirectory();
//                             
//                     int dot = pathName.getPath().lastIndexOf(".");
//                     int sep = pathName.getPath().lastIndexOf(File.separator);
//                     
//                     def fileName =  pathName.getPath().substring(sep + 1, dot);
//                               
//                     def nameAccept = startsWith=="" || fileName.startsWith(startsWith)
//                     
//                     return fileAccept && nameAccept
//                }
//            })) {
//                if (f.isFile()) {
//                        result.add(f);
//                }
//            }
//
//        } else {
//            //
//            throw new RuntimeException("Viewer: path '"+dir.getName()+"' not found.");
//        }
//       
//        return result[result.size()-1]
//   }
}
