/* original code from:
 * http://vtk.org/Wiki/VTK/Examples/CSharp/PolyData/VectorFieldNonZeroExtraction
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.annotation.ComponentInfo;
import eu.mihosoft.vrl.annotation.ParamInfo;
import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import vtk.vtkActor;
import vtk.vtkArrowSource;
import vtk.vtkCubeSource;
import vtk.vtkGlyph2D;
import vtk.vtkGlyph3D;
import vtk.vtkImageData;
import vtk.vtkImageMagnitude;
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

    public Visualization VectorFieldNonZeroExtraction(
            @ParamInfo(style="load-dialog")
                    File file,
            int elementInFile,
            @ParamInfo(name="threshold",options="value=0.00001")
            double threshold) {
        
//        // Create an image
//        vtkImageData image = new vtkImageData();
//        
//        CreateVectorField(image);
        
        vtkXMLUnstructuredGridReader reader = new vtkXMLUnstructuredGridReader();

//            System.out.println("-- reader = "+ reader);
            reader.SetFileName(file.getAbsolutePath());
            reader.Update();
            vtkUnstructuredGrid image = reader.GetOutput();
            

//        // This filter produces a vtkImageData with an array named "Magnitude"
//        vtkImageMagnitude magnitudeFilter = new vtkImageMagnitude();
//        magnitudeFilter.SetInputConnection(image.GetProducerPort());
//        magnitudeFilter.Update();
//
//        image.GetPointData().AddArray(magnitudeFilter.GetOutput().GetPointData().GetScalars());
//        image.GetPointData().SetActiveScalars("Magnitude");

        vtkThresholdPoints thresholdVector = new vtkThresholdPoints();
        thresholdVector.SetInput(image);
        thresholdVector.SetInputArrayToProcess(
                elementInFile,
                image.GetInformation());

        thresholdVector.ThresholdByUpper(threshold);
        thresholdVector.Update();

        // in case you want to save imageData
        //vtkXMLPolyDataWriter writer = vtkXMLPolyDataWriter.New();
        //writer.SetFileName("output.vtp");
        //writer.SetInputConnection(thresholdPoints.GetOutputPort());
        //writer.Write();

        // repesents the pixels
        vtkCubeSource cubeSource = new vtkCubeSource();
        cubeSource.SetXLength(0.01);
        cubeSource.SetYLength(0.01);
        cubeSource.SetZLength(0.01);
        
        vtkGlyph3D glyph = new vtkGlyph3D();
        glyph.SetInput(image);
        glyph.SetSourceConnection(cubeSource.GetOutputPort()); //show cubes
        // don't scale glyphs according to any scalar data
        glyph.SetScaleModeToDataScalingOff();

        vtkPolyDataMapper glyphMapper = new vtkPolyDataMapper();
        glyphMapper.SetInputConnection(glyph.GetOutputPort());
        // don't color glyphs according to scalar data
        glyphMapper.ScalarVisibilityOff();
        glyphMapper.SetScalarModeToDefault();

        vtkActor actor = new vtkActor();
        actor.SetMapper(glyphMapper);

        // represent vector field
        vtkGlyph3D vectorGlyph = new vtkGlyph3D();
        vtkArrowSource arrowSource = new vtkArrowSource();
        vtkPolyDataMapper vectorGlyphMapper = new vtkPolyDataMapper();

        int n = image.GetPointData().GetNumberOfArrays();
        for (int i = 0; i < n; i++) {
            System.out.println("name of array[" + i + "]: " + image.GetPointData().GetArrayName(i));
        }

        vtkPolyData tmp = thresholdVector.GetOutput();
        System.out.println("number of thresholded points: " + tmp.GetNumberOfPoints());
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
        vectorGlyph.SetInputArrayToProcess(
                1,
                image.GetInformation());

        vectorGlyph.Update();

        vectorGlyphMapper.SetInputConnection(vectorGlyph.GetOutputPort());
        vectorGlyphMapper.Update();

        vtkActor vectorActor = new vtkActor();
        vectorActor.SetMapper(vectorGlyphMapper);


        Visualization vis = new Visualization();
        vis.addActor(actor);
        vis.addActor(vectorActor);

        return vis;
    }

    void CreateVectorField(vtkImageData image) {
        // Specify the size of the image data
        image.SetDimensions(3, 3, 3);
        image.SetNumberOfScalarComponents(3);
        image.SetScalarTypeToDouble();
        image.AllocateScalars();
        image.SetSpacing(10.0, 10.0, 10.0);
        int[] dims = image.GetDimensions();

        double[] pixel = new double[]{0.0, 0.0, 0.0};
        double[] pPixel;

        // Zero the vectors
        for (int z = 0; z < dims[2]; z++) {
            for (int y = 0; y < dims[1]; y++) {
                for (int x = 0; x < dims[0]; x++) {
                    pPixel = image.GetPoint(image.FindPoint(x, y, 0));
                    pPixel = Arrays.copyOfRange(pixel, 0, 3);
                }
            }
        }

        // Set two of the pixels to non zero values
        pixel[0] = 8.0f;
        pixel[1] = 8.0f;
        pixel[2] = -8.0f;
        pPixel = image.GetPoint(image.FindPoint(0, 2, 0));
        pPixel = Arrays.copyOfRange(pixel, 0, 3);

        pixel[0] = 8.0f;
        pixel[1] = -8.0f;
        pixel[2] = 8.0f;
        pPixel = image.GetPoint(image.FindPoint(2, 0, 2));
        pPixel = Arrays.copyOfRange(pixel, 0, 3);

    }
}
