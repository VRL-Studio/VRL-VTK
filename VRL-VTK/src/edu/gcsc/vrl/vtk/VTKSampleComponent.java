/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;


import eu.mihosoft.vrl.annotation.ComponentInfo;
import eu.mihosoft.vrl.annotation.MethodInfo;
import vtk.*;

/**
 * 
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
@ComponentInfo(name="VTKSample", category="VTK")
public class VTKSampleComponent implements java.io.Serializable {
	private static final long serialVersionUID=1L;

    private transient vtk.vtkActor cutActor;
    private transient vtk.vtkActor isoActor;

	// add your code here
    @MethodInfo(hide=false, callOptions="autoinvoke")
    public Visualization visualize() {
         double radius = 0.8;        
         /*
         * sphere radius
         */

        /**
         * ** 1) INPUT DATA: Sphere Implicit Function ***
         */
        vtkSphere sphere = new vtkSphere();
        sphere.SetRadius(radius);

        vtkSampleFunction sample = new vtkSampleFunction();
        sample.SetSampleDimensions(50, 50, 50);
        sample.SetImplicitFunction(sphere);

        /**
         * ** 2) PIPELINE 1: Isosurface Actor ***
         */
        /*
         * contour filter - will generate isosurfaces from 3D data
         */
        vtkContourFilter contour = new vtkContourFilter();
        contour.SetInputConnection(sample.GetOutputPort());
        contour.GenerateValues(3, 0, 1);

        /*
         * mapper, translates polygonal representation to graphics primitives
         */
        vtkPolyDataMapper isoMapper = new vtkPolyDataMapper();
        isoMapper.SetInputConnection(contour.GetOutputPort());

        /*
         * isosurface actor
         */
        isoActor = new vtkActor();
        isoActor.SetMapper(isoMapper);

        /**
         * ** 3) PIPELINE 2: Cutting Plane Actor ***
         */
        /*
         * define a plane in x-y plane and passing through the origin
         */
        vtkPlane plane = new vtkPlane();
        plane.SetOrigin(0, 0, 0);
        plane.SetNormal(0, 0, 1);

        /*
         * cutter, basically interpolates source data onto the plane
         */
        vtkCutter planeCut = new vtkCutter();
        planeCut.SetInputConnection(sample.GetOutputPort());
        planeCut.SetCutFunction(plane);
        /*
         * this will actually create 3 planes at the subspace where the implicit
         * function evaluates to -0.7, 0, 0.7 (0 would be original plane). In
         * our case this will create three x-y planes passing through z=-0.7,
         * z=0, and z=+0.7
         */
        planeCut.GenerateValues(3, -0.7, 0.7);

        /*
         * look up table, we want to reduce number of values to get discrete
         * bands
         */
        vtkLookupTable lut = new vtkLookupTable();
        lut.SetNumberOfTableValues(5);

        /*
         * mapper, using our custom LUT
         */
        vtkPolyDataMapper cutMapper = new vtkPolyDataMapper();
        cutMapper.SetInputConnection(planeCut.GetOutputPort());
        cutMapper.SetLookupTable(lut);

        /*
         * cutting plane actor, looks much better with flat shading
         */
        cutActor = new vtkActor();
        cutActor.SetMapper(cutMapper);
        cutActor.GetProperty().SetInterpolationToFlat();

        /**
         * ** 4) PIPELINE 3: Surface Geometry Actor ***
         */
        /*
         * create polygonal representation of a sphere
         */
        vtkSphereSource surf = new vtkSphereSource();
        surf.SetRadius(radius);

        /*
         * another mapper
         */
        vtkPolyDataMapper surfMapper = new vtkPolyDataMapper();
        surfMapper.SetInputConnection(surf.GetOutputPort());

        /*
         * surface geometry actor, turn on edges and apply flat shading
         */
        vtkActor surfActor = new vtkActor();
        surfActor.SetMapper(surfMapper);
        surfActor.GetProperty().EdgeVisibilityOn();
        surfActor.GetProperty().SetEdgeColor(0.2, 0.2, 0.2);
        surfActor.GetProperty().SetInterpolationToFlat();

        return new Visualization(surfActor, isoActor, cutActor);
    }
}

