/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.animation.LinearTarget;
import eu.mihosoft.vrl.v3d.Node;
import eu.mihosoft.vrl.v3d.Nodes;
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

        LinearTarget red = new LinearTarget(colorOne.getRed(), colorTwo.getRed());
        LinearTarget green = new LinearTarget(colorOne.getGreen(), colorTwo.getGreen());
        LinearTarget blue = new LinearTarget(colorOne.getBlue(), colorTwo.getBlue());

        VTriangleArray triangleArray = new VTriangleArray();

        int previousOffset = 0;
        int connectivityOffset = 0;

        for (int i = 0; i < offsets.length; i++) {

            int type = types[i];

            int elementSize = offsets[i] - previousOffset;
            previousOffset = offsets[i];

            Node[] nodes = new Node[elementSize];

            for (int j = 0; j < elementSize; j++) {
                int pointIndex = connectivity[connectivityOffset + j];

                float color = colors[pointIndex];

                float colorScale = 1.f / (cMax - cMin);

                red.step(color*colorScale);
                blue.step(color*colorScale);
                green.step(color*colorScale);

                float x = points[pointIndex][0];
                float y = points[pointIndex][1];
                float z = points[pointIndex][2];

                nodes[j] =
                        new Node(x, y, z,
                        new Color3f(new Color(
                        (int) red.getValue(),
                        (int) green.getValue(),
                        (int) blue.getValue())));
            }

            connectivityOffset += elementSize;

            // we only support triangles and quads
            // (quads are represented as two triangles)
            if (type == 5 || type == 9) {
                if (elementSize == 3) {
                    triangleArray.addTriangle(
                            new Triangle(nodes[0], nodes[1], nodes[2]));
                }

                if (elementSize == 4) {
                    triangleArray.addTriangle(
                            new Triangle(nodes[0], nodes[1], nodes[2]));
                    triangleArray.addTriangle(
                            new Triangle(nodes[0], nodes[2], nodes[3]));
                }
            }

        }

        triangleArray.centerNodes();

        VGeometry3D result = new VGeometry3D(
                triangleArray, Color.black, Color.white, 1.f, true, true);

        return result;
    }
}
