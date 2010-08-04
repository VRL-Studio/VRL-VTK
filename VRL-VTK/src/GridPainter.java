/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



import eu.mihosoft.vrl.animation.LinearTarget;
import eu.mihosoft.vrl.visual.ImageUtils;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class GridPainter {
    private Color colorOne = Color.blue;
    private Color colorTwo = Color.red;

    public void paint(Graphics2D g2, UnstructuredGrid grid) {
        DataArray pointArray = null;

        DataArray colorArray = null;

        for (DataArray a : grid.getArrays()) {
            System.out.println("Array: " + a.getName() + ", " + a.getType() + ", #Comp: " + a.getNumberOfComponents());
            if (a.getNumberOfComponents()==3) {
                pointArray = a;
            }

            if (a.getName().equals("ndata00")) {
                colorArray = a;
            }
        }


        float[] data = (float[]) pointArray.getDataDecoder().getArray();

        int numberOfComponents = pointArray.getNumberOfComponents();
        int numberOfPoints = data.length/numberOfComponents;

        float[][] points =
                new float[numberOfPoints][numberOfComponents];

        for (int i = 0; i < data.length; i++) {
            points[i/numberOfComponents][i%numberOfComponents] = data[i];
        }

        float[] colors = (float[]) colorArray.getDataDecoder().getArray();


        float xMin = Float.MAX_VALUE;
        float yMin = Float.MAX_VALUE;

        float xMax = Float.MIN_VALUE;
        float yMax = Float.MIN_VALUE;

        for (float[] p : points){
            xMin = Math.min(xMin, p[0]);
            yMin = Math.min(yMin, p[1]);

            xMax = Math.max(xMax, p[0]);
            yMax = Math.max(yMax, p[1]);
        }

        float xLength = Math.abs(xMax-xMin);
        float yLength = Math.abs(yMax-yMin);

        float scaleX = g2.getDeviceConfiguration().getBounds().width/xLength;
        float scaleY = g2.getDeviceConfiguration().getBounds().height/yLength;

        float offsetX = 0 + xMin;
        float offsetY = 0 + yMin;

        System.out.println("X_MIN: " + xMin);
        System.out.println("X_MAX: " + xMax);

        System.out.println("Y_MIN: " + yMin);
        System.out.println("Y_MAX: " + yMax);


        float cMin = Float.MAX_VALUE;
        float cMax = Float.MIN_VALUE;

        for (float c : colors){
            cMin = Math.min(cMin, c);
            cMax = Math.max(cMax, c);
        }

        double cLength = Math.abs(cMax-cMin);

        double scaleColor = 1.d/cLength;

        System.out.println("C_MIN: " + cMin);
        System.out.println("C_MAX: " + cMax);
        System.out.println("C_SCALE: " + scaleColor);



//        BufferedImage img = ImageUtils.createCompatibleImage(2048, 2048);
//
//        Graphics2D g2 = img.createGraphics();



        Color colorOne = Color.blue;
        Color colorTwo = Color.red;

        g2.setColor(colorOne);
        g2.fillRect(0, 0, 2048, 2048);

        LinearTarget red = new LinearTarget(colorOne.getRed(), colorTwo.getRed());
        LinearTarget green = new LinearTarget(colorOne.getGreen(), colorTwo.getGreen());
        LinearTarget blue = new LinearTarget(colorOne.getBlue(), colorTwo.getBlue());


        for (int i = 0; i < numberOfPoints;i++) {
            float xOrig = points[i][0];
            float yOrig = points[i][1];
            float zOrig = points[i][2];

            float cOrig = colors[i];

            int x = (int) (xOrig * scaleX+offsetX);
            int y = (int) (yOrig * scaleY+offsetY);

            double v = (double) (cOrig * scaleColor);

            //System.out.println("c: " + v);

            red.step(v);
            green.step(v);
            blue.step(v);

            int r = (int) red.getValue();
            int g = (int) green.getValue();
            int b = (int) blue.getValue();

            //System.out.println("r: " + r + ", g: " + g + ", b: " + b);


            g2.setColor(new Color(r,g,b));

            g2.fillRect(x, y, 16, 7);

        }
    }

    public BufferedImage paint(int w, int h, UnstructuredGrid grid) {
        BufferedImage result = ImageUtils.createCompatibleImage(w, h);

        Graphics2D g2 = result.createGraphics();

        paint(g2, grid);

        g2.dispose();

        return result;
    }

    public void paint (BufferedImage img, UnstructuredGrid grid) {
        Graphics2D g2 = img.createGraphics();

        paint(g2, grid);

        g2.dispose();
    }


    /**
     * @return the colorOne
     */
    public Color getColorOne() {
        return colorOne;
    }

    /**
     * @param colorOne the colorOne to set
     */
    public void setColorOne(Color colorOne) {
        this.colorOne = colorOne;
    }

    /**
     * @return the colorTwo
     */
    public Color getColorTwo() {
        return colorTwo;
    }

    /**
     * @param colorTwo the colorTwo to set
     */
    public void setColorTwo(Color colorTwo) {
        this.colorTwo = colorTwo;
    }
}
