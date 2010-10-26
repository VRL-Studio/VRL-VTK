/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.animation.LinearTarget;
import eu.mihosoft.vrl.reflection.MethodInfo;
import eu.mihosoft.vrl.reflection.ParamInfo;
import eu.mihosoft.vrl.v3d.Node;
import eu.mihosoft.vrl.v3d.Triangle;
import eu.mihosoft.vrl.v3d.VGeometry3D;
import eu.mihosoft.vrl.v3d.VTriangleArray;
import java.awt.Color;
import java.io.File;
import java.io.Serializable;
import javax.vecmath.Color3f;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class GridPainter3D implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 
     * @param colorOne
     * @param colorTwo
     * @param grid
     * @return
     */
    @MethodInfo(hide=true)
    public VGeometry3D paint(
            Color colorOne, Color colorTwo,
            UnstructuredGrid grid, String colorArrayName) {
        return paint(colorOne, colorTwo, grid, 20, colorArrayName);
    }

    /**
     *
     * @param colorOne
     * @param colorTwo
     * @param grid
     * @return
     */
    @MethodInfo()
    public VGeometry3D paint(
            Color colorOne, Color colorTwo,
            @ParamInfo(style="load-dialog") File f, String colorArrayName) {
        return paint(colorOne, colorTwo, new UnstructuredGrid(f), 20, colorArrayName);
    }

    /**
     *
     * @param colorOne
     * @param colorTwo
     * @param grid
     * @return
     */
    public VGeometry3D paint(
            Color colorOne, Color colorTwo,
            UnstructuredGrid grid) {
        return paint(colorOne, colorTwo, grid, 20, "ndata000");
    }

    /**
     *
     * @param colorOne
     * @param colorTwo
     * @param grid
     * @param maxLength
     * @return
     */
    public VGeometry3D paint(
            Color colorOne, Color colorTwo,
            UnstructuredGrid grid, float maxLength, String colorArrayName) {

        // evaluate data arrays and convert array data
        float[] pointData = null;
        float[] colors = null;
        int[] offsets = null;
        int[] connectivity = null;
        byte[] types = null;

        for (DataArray a : grid.getArrays()) {
            System.out.println("Array: " + a.getName() + ", "
                    + a.getType() + ", #Comp: " + a.getNumberOfComponents());
            if (a.getNumberOfComponents() == 3) {
                pointData = (float[]) a.getDataDecoder().getArray();
                System.out.println("POINTS");
            }

            if (a.getName().equals("connectivity")) {
                connectivity = (int[]) a.getDataDecoder().getArray();
            }

            if (a.getName().equals("offsets")) {
                offsets = (int[]) a.getDataDecoder().getArray();
            }

            if (a.getName().equals("types")) {
                types = (byte[]) a.getDataDecoder().getArray();
            }

            if (a.getName().equals(colorArrayName)) {
                colors = (float[]) a.getDataDecoder().getArray();
            }
        }

        // convert point data from one dimensional array to two dimensional
        // point array

        int numberOfComponents = 3;
        int numberOfPoints = pointData.length / numberOfComponents;

        float[][] points =
                new float[numberOfPoints][numberOfComponents];

        for (int i = 0; i < pointData.length; i++) {
            points[i / numberOfComponents][i % numberOfComponents] = pointData[i];
        }

        // find min max coordinate values etc.

        float xMin = Float.MAX_VALUE;
        float yMin = Float.MAX_VALUE;
        float zMin = Float.MAX_VALUE;

        float xMax = Float.MIN_VALUE;
        float yMax = Float.MIN_VALUE;
        float zMax = Float.MIN_VALUE;


        for (float[] p : points) {

            for (int i = 0; i < 3; i++) {
                if (p[i] == Float.floatToIntBits(Float.NaN)) {
                    p[i]=0;
                }
            }


            xMin = Math.min(xMin, p[0]);
            yMin = Math.min(yMin, p[1]);
            zMin = Math.min(zMin, p[2]);

            xMax = Math.max(xMax, p[0]);
            yMax = Math.max(yMax, p[1]);
            zMax = Math.max(zMax, p[2]);


            //System.out.println("P[0]: " + p[0] + " P[1]: " + p[1] + " P[2]: " + p[2]);
        }

        float xLength = Math.abs(xMax - xMin);
        float yLength = Math.abs(yMax - yMin);
        float zLength = Math.abs(zMax - zMin);

        float offsetX = (float) xMin;
        float offsetY = (float) yMin;
        float offsetZ = (float) zMin;

        System.out.println("X_MIN: " + xMin);
        System.out.println("X_MAX: " + xMax);

        System.out.println("Y_MIN: " + yMin);
        System.out.println("Y_MAX: " + yMax);

        System.out.println("Z_MIN: " + yMin);
        System.out.println("Z_MAX: " + yMax);

        System.out.println("X_OFFSET: " + offsetX);
        System.out.println("Y_OFFSET: " + offsetY);


        // compute color scale

        float cMin = Float.MAX_VALUE;
        float cMax = Float.MIN_VALUE;

        for (float c : colors) {
            cMin = Math.min(cMin, c);
            cMax = Math.max(cMax, c);
        }

        double cLength = Math.abs(cMax - cMin);
        double scaleColor = 1.d / cLength;

        System.out.println("C_MIN: " + cMin);
        System.out.println("C_MAX: " + cMax);
        System.out.println("C_SCALE: " + scaleColor);


        // define linear color interpolators

        LinearTarget red =
                new LinearTarget(colorOne.getRed(), colorTwo.getRed());
        LinearTarget green =
                new LinearTarget(colorOne.getGreen(), colorTwo.getGreen());
        LinearTarget blue =
                new LinearTarget(colorOne.getBlue(), colorTwo.getBlue());

        // geometry

        VTriangleArray triangleArray = new VTriangleArray();

        // the previous offset from the offsets data array
        // which is used to compute the current element size
        // (number of points per element)
        int previousOffset = 0;

        // offset for the connectivity array
        int connectivityOffset = 0;

        // compute geometry scale
        float scale = maxLength / Math.max(Math.max(xLength, yLength), zLength);

        for (int i = 0; i < offsets.length; i++) {

            int type = types[i];

            // element size defines the number of points of an element
            int elementSize = offsets[i] - previousOffset;

            previousOffset = offsets[i];

            // stores the points of the current element
            Node[] nodes = new Node[elementSize];

            for (int j = 0; j < elementSize; j++) {

                // the connectivity array contains the point indices of this
                // element
                int pointIndex = connectivity[connectivityOffset + j];

//                cMax = 30;

                // compute color (linear color scale)
                float color = colors[pointIndex] - cMin;
                float colorScale = 1.f / Math.abs(cMax - cMin);

                red.step(color * colorScale);
                blue.step(color * colorScale);
                green.step(color * colorScale);

                // translate values (center is (0,0,0))
                float x = points[pointIndex][0] - offsetX - xLength / 2.f;
                float y = points[pointIndex][1] - offsetY - yLength / 2.f;
                float z = points[pointIndex][2] - offsetZ - zLength / 2.f;

                // scale values
                x *= scale;
                y *= scale;
                z *= scale;

                int r = (int) red.getValue();
                int g = (int) green.getValue();
                int b = (int) blue.getValue();

                try {
                    // create node
                    nodes[j] =
                            new Node(x, y, z,
                            new Color3f(new Color(r, g, b)));
                } catch (Exception ex) {
                    System.out.println(
                            ">> color values: c="
                            + color * colorScale
                            + ", r=" + r + ", g=" + g + ", b=" + b);
                }
            }

            connectivityOffset += elementSize;

            // we only support triangles (type 5) and quads (type 9)
            // (quads are represented by two triangles)
            // otherwise we do nothing (the current element will be ignored)
            if (type == 5 || type == 9) {

                // triangle
                if (elementSize == 3) {
                    triangleArray.addTriangle(
                            new Triangle(nodes[0], nodes[1], nodes[2]));
                }

                // quad
                if (elementSize == 4) {
                    triangleArray.addTriangle(
                            new Triangle(nodes[0], nodes[1], nodes[2]));
                    triangleArray.addTriangle(
                            new Triangle(nodes[0], nodes[2], nodes[3]));
                }
            }

        }

        // create the final geometry (with vertex coloring)
        VGeometry3D result = new VGeometry3D(
                triangleArray, Color.black, Color.white, 1.f, true, true);

        return result;
    }
}
