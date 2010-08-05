/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.animation.LinearTarget;
import eu.mihosoft.vrl.v3d.Node;
import eu.mihosoft.vrl.v3d.Triangle;
import eu.mihosoft.vrl.v3d.VGeometry3D;
import eu.mihosoft.vrl.v3d.VTriangleArray;
import java.awt.Color;
import java.io.Serializable;
import javax.vecmath.Color3f;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class GridPainter3D implements Serializable {

    private static final long serialVersionUID = 1L;

    public VGeometry3D paint(
            Color colorOne, Color colorTwo, UnstructuredGrid grid) {
        int width = 50;
        int height = 50;

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

            if (a.getName().equals("ndata00")) {
                colors = (float[]) a.getDataDecoder().getArray();
            }
        }

        int numberOfComponents = 3;
        int numberOfPoints = pointData.length / numberOfComponents;

        float[][] points =
                new float[numberOfPoints][numberOfComponents];

        for (int i = 0; i < pointData.length; i++) {
            points[i / numberOfComponents][i % numberOfComponents] = pointData[i];
        }

        float xMin = Float.MAX_VALUE;
        float yMin = Float.MAX_VALUE;

        float xMax = Float.MIN_VALUE;
        float yMax = Float.MIN_VALUE;

        for (float[] p : points) {
            xMin = Math.min(xMin, p[0]);
            yMin = Math.min(yMin, p[1]);

            xMax = Math.max(xMax, p[0]);
            yMax = Math.max(yMax, p[1]);
        }

//        float xLength = Math.abs(xMax - xMin);
//        float yLength = Math.abs(yMax - yMin);

//        float scaleX = width / xLength;
//        float scaleY = height / yLength;

        float offsetX = (float) (0 + xMin);
        float offsetY = (float) (0 + yMin);

        System.out.println("X_MIN: " + xMin);
        System.out.println("X_MAX: " + xMax);

        System.out.println("Y_MIN: " + yMin);
        System.out.println("Y_MAX: " + yMax);

        System.out.println("X_OFFSET: " + offsetX);
        System.out.println("Y_OFFSET: " + offsetY);

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

//        LinearTarget red = new LinearTarget(colorOne.getRed(), colorTwo.getRed());
//        LinearTarget green = new LinearTarget(colorOne.getGreen(), colorTwo.getGreen());
//        LinearTarget blue = new LinearTarget(colorOne.getBlue(), colorTwo.getBlue());

        VTriangleArray triangleArray = new VTriangleArray();

        VGeometry3D result = new VGeometry3D();

        int previousOffset = 0;

        for (int i = 0; i < offsets.length; i++) {

            int type = types[i];

            int elementSize = offsets[i] - previousOffset;
            previousOffset = offsets[i];

            Node[] nodes = new Node[elementSize];

            for (int j = 0; j < elementSize; j++) {
                int pointIndex = connectivity[i] * 3; // 3 represents numberOfComponents

                float color = colors[pointIndex];

//                System.out.println(
//                        "P: X="
//                        + pointData[pointIndex]
//                        + ", Y=" + pointData[pointIndex + 1] +
//                        ", Z=" + pointData[pointIndex + 2]);

                float colorScale = 1.f / (cMax - cMin);

                nodes[j] =
                        new Node(
                        pointData[pointIndex],
                        pointData[pointIndex + 1],
                        pointData[pointIndex + 2],
                        new Color3f(color*colorScale,0,0));
            }

            if (type == 5) {
                triangleArray.
                        addTriangle(new Triangle(nodes[0], nodes[1], nodes[2]));
            }


        }



//        for (int i = 0; i < offsets.length; i++) {
//            int offset = offsets[i];
//
//            int pointIndex = connectivity[i];
//
//            int type = types[pointIndex];
//
//            int numberOfNodes = 0;
//
//            boolean unsupported = false;
//
//            switch (type) {
//                case 0:
//                    System.err.println(
//                            ">> type " + type + " is an illegal element type!");
//                    break;
//                case 5:
//                    numberOfNodes = 3;
//                    break;
//                case 9:
//                    numberOfNodes = 4;
//                    break;
//                default:
//                    unsupported = true;
//                    System.err.println(">> type " + type + " is unsupported!");
//                    break;
//            }
//
//            // element not supported
//            if (unsupported) {
//                continue;
//            }
//
//            Triangle[] triangles =
//                    createElementTriangles(
//                    offset, type, numberOfNodes, points, connectivity, colors, cMin, cMax);
//
//            if (triangles != null) {
//                for (Triangle t : triangles) {
//                    if (t != null) {
//                        triangleArray.addTriangle(t);
//                    }
//                }
//            }

//        }

        return result;
    }

    private Triangle[] createElementTriangles(int offset,
            int type, float[][] points, int[] connectivity,
            float[] colors, float minColor, float maxColor) {

        Triangle[] result = null;

        float colorScale = 1.f / (maxColor - minColor);

        if (type == 5) {
            result = new Triangle[1];

            Node[] nodes = new Node[3];

            for (int i = offset; i < offset + 3; i++) {

                int pointIndex = connectivity[i / 3];

                float x = points[pointIndex][0];
                float y = points[pointIndex][1];
                float z = points[pointIndex][2];

                float color = colors[pointIndex] * colorScale;

                nodes[i - offset] =
                        new Node(type, x, y, z, new Color3f(color, 0, 0));
            }

            result[0] = new Triangle(nodes[0], nodes[1], nodes[2]);
        }
//        else if (type == 9) {
//            result = new Triangle[2]; // split quad into two triangles
//
//            // first triangle
//            Node[] nodesFirst = new Node[3];
//
//            for (int i = offset; i < offset + 3; i++) {
////                System.out.println("INDEX: " + (i -offset));
//                float x = points[i/3][0];
//                float y = points[i/3][1];
//                float z = points[i/3][2];
//
//                float color = colors[i/3] * colorScale;
//
//                nodesFirst[i - offset] =
//                        new Node(type, x, y, z, new Color3f(color, 0, 0));
//            }
//
//            result[0] =
//                    new Triangle(nodesFirst[0], nodesFirst[1], nodesFirst[2]);
//
//            // second triangle
//            Node[] nodesSecond = new Node[3];
//
//            for (int i = offset +2; i < offset + 4; i++) {
//                float x = points[i/3][0];
//                float y = points[i/3][1];
//                float z = points[i/3][2];
//
//                float color = colors[i/3] * colorScale;
//
////                System.out.println("INDEX: " + (i -offset));
//
//                nodesSecond[i - (offset +2)] =
//                        new Node(type, x, y, z, new Color3f(color, 0, 0));
//            }
//
//            result[1] =
//                    new Triangle(nodesSecond[0], nodesSecond[1], nodesSecond[2]);
//        }

        return result;
    }
}
