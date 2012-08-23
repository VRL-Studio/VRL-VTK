/* original code from:
 * http://vtk.org/Wiki/VTK/Examples/CSharp/PolyData/VectorFieldNonZeroExtraction
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.annotation.ComponentInfo;
import eu.mihosoft.vrl.annotation.ParamInfo;
import java.io.File;
import java.io.Serializable;
import vtk.vtkActor;
import vtk.vtkArrowSource;
import vtk.vtkCubeSource;
import vtk.vtkGlyph3D;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkThresholdPoints;
import vtk.vtkUnstructuredGrid;
import vtk.vtkXMLUnstructuredGridReader;

/**
 *
 * @author Christian Poliwoda <christian.poliwoda@gcsc.uni-frankfurt.de>
 */
@ComponentInfo(name = "VectorFieldExample", category = "Custom")
public class VectorFieldExample implements Serializable {

    private static final long serialVersionUID = 1L;

    public Visualization showVectorField(
            @ParamInfo(style = "load-dialog") File file,
            int elementInFile,
            @ParamInfo(name = "threshold", options = "value=0.00001") double threshold,
            @ParamInfo(name = "scaleFactor", options = "value=0.05") double scaleFactor) {

        Visualization vis = new Visualization();


//        // Create an image
//        vtkImageData image = new vtkImageData();
//        
//        CreateVectorField(image);

        vtkXMLUnstructuredGridReader reader = new vtkXMLUnstructuredGridReader();

//            System.out.println("-- reader = "+ reader);
        reader.SetFileName(file.getAbsolutePath());
        reader.Update();
        vtkUnstructuredGrid image = reader.GetOutput();
        image.GetPointData().SetVectors(image.GetPointData().GetArray(elementInFile));


//        // This filter produces a vtkImageData with an array named "Magnitude"
//        vtkImageMagnitude magnitudeFilter = new vtkImageMagnitude();
//        magnitudeFilter.SetInputConnection(image.GetProducerPort());
//        magnitudeFilter.Update();
//
//        image.GetPointData().AddArray(magnitudeFilter.GetOutput().GetPointData().GetScalars());
//        image.GetPointData().SetActiveScalars("Magnitude");

        
//        vtkThresholdPoints thresholdVector = new vtkThresholdPoints();
//        thresholdVector.SetInput(image);
//        thresholdVector.SetInputArrayToProcess(
//                elementInFile,
//                image.GetInformation());
//
//        thresholdVector.ThresholdByUpper(threshold);
//        thresholdVector.Update();
//        
//        vtkPolyData tmp = thresholdVector.GetOutput();
//        System.out.println("number of thresholded points: " + tmp.GetNumberOfPoints());

        
        // in case you want to save imageData
        //vtkXMLPolyDataWriter writer = vtkXMLPolyDataWriter.New();
        //writer.SetFileName("output.vtp");
        //writer.SetInputConnection(thresholdPoints.GetOutputPort());
        //writer.Write();

        // repesents the pixels
//        vtkCubeSource cubeSource = new vtkCubeSource();
//        cubeSource.SetXLength(0.01);
//        cubeSource.SetYLength(0.01);
//        cubeSource.SetZLength(0.01);

//        vtkGlyph3D glyph = new vtkGlyph3D();
//        glyph.SetInput(image);
////        glyph.SetSourceConnection(cubeSource.GetOutputPort()); //show cubes
//        // don't scale glyphs according to any scalar data
////        glyph.SetScaleModeToDataScalingOff();
//        glyph.SetSource(ar.GetPort());
//
//        vtkPolyDataMapper glyphMapper = new vtkPolyDataMapper();
//        glyphMapper.SetInputConnection(glyph.GetOutputPort());
//        // don't color glyphs according to scalar data
////        glyphMapper.ScalarVisibilityOff();
////        glyphMapper.SetScalarModeToDefault();
//        glyphMapper.SetScalarModeToUsePointData();
//
//        vtkActor actor = new vtkActor();
//        actor.SetMapper(glyphMapper);
//
//        vis.addActor(actor);




        // represent vector field
        vtkGlyph3D vectorGlyph = new vtkGlyph3D();
        vtkArrowSource arrowSource = new vtkArrowSource();
        vtkPolyDataMapper vectorGlyphMapper = new vtkPolyDataMapper();

        int n = image.GetPointData().GetNumberOfArrays();
        for (int i = 0; i < n; i++) {
            System.out.println("name of array[" + i + "]: " + image.GetPointData().GetArrayName(i));
        }

//        vectorGlyph.SetInputConnection(thresholdVector.GetOutputPort());
//        vectorGlyph.SetInputConnection(thresholdVector.GetOutputPort());

//        vtkGlyph2D vectorfieldVector = new vtkGlyph2D();
//        
//        vectorfieldVector.SetInput(image);
//        vectorfieldVector.SetInputArrayToProcess(
//                elementInFile,
//                image.GetInformation());
//
//        
//        vectorfieldVector.Update();

        vectorGlyph.SetInputConnection(image.GetProducerPort());


        // in case you want the point glyphs to be oriented according to 
        // scalar values in array "ImageScalars" uncomment the following line
//        image.GetPointData().SetActiveVectors("ImageScalars");

//        image.GetPointData().SetActiveVectors(image.GetPointData().GetArrayName(elementInFile));

        vectorGlyph.SetSourceConnection(arrowSource.GetOutputPort());
        vectorGlyph.SetScaleModeToScaleByVector();
        vectorGlyph.SetVectorModeToUseVector();
        vectorGlyph.ScalingOn();
        vectorGlyph.OrientOn();
        vectorGlyph.SetColorModeToColorByVector();//color the glyphs
        
        vectorGlyph.SetInputArrayToProcess(
                elementInFile,
                image.GetInformation());

        vectorGlyph.SetScaleFactor(scaleFactor);

        vectorGlyph.Update();

        vectorGlyphMapper.SetInputConnection(vectorGlyph.GetOutputPort());
        vectorGlyphMapper.Update();

        vtkActor vectorActor = new vtkActor();
        vectorActor.SetMapper(vectorGlyphMapper);

        vis.addActor(vectorActor);

        return vis;
    }
}
