/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.gcsc.vrl.playground;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.universe.SimpleUniverse;

import javax.media.j3d.*;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import java.applet.Applet;
import java.awt.*;

/**
 * TriColor: adding some color
 *
 * @author Kevin J. Duling (kevin@duling.us)
 */
public final class TriColor
    extends Applet
{
  /**
   * Default constructor.  Here we create the universe.
   */
  public TriColor()
  {
    setLayout(new BorderLayout());
    Canvas3D canvas = createCanvas();
    add("Center", canvas);
    SimpleUniverse u = new SimpleUniverse(canvas);
    BranchGroup scene = createContent();
    u.getViewingPlatform().setNominalViewingTransform();  // back away from object a little
    scene.compile();
    u.addBranchGraph(scene);
  }

  /**
   * Create a canvas to draw the 3D world on.
   */
  private Canvas3D createCanvas()
  {
    GraphicsConfigTemplate3D graphicsTemplate = new GraphicsConfigTemplate3D();
    GraphicsConfiguration gc1 =
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice().getBestConfiguration(graphicsTemplate);
    return new Canvas3D(gc1);
  }

  /**
   * Fill your 3D world with content
   */
  private BranchGroup createContent()
  {
    BranchGroup objRoot = new BranchGroup();
    // Create a triangle with each point a different color.  Remember to
    // draw the points in counter-clockwise order.  That is the default
    // way of determining which is the front of a polygon.
    //        o (1)
    //       / \
    //      /   \
    // (2) o-----o (0)
    Shape3D shape = new Shape3D();
    TriangleArray tri = new TriangleArray(3, TriangleArray.COORDINATES | TriangleArray.COLOR_3);
    tri.setCoordinate(0, new Point3f(0.5f, 0.0f, 0.0f));
    tri.setCoordinate(1, new Point3f(0.0f, 0.5f, 0.0f));
    tri.setCoordinate(2, new Point3f(-0.5f, 0.0f, 0.0f));
    tri.setColor(0, new Color3f(1.0f, 0.0f, 0.0f));
    tri.setColor(1, new Color3f(0.0f, 1.0f, 0.0f));
    tri.setColor(2, new Color3f(0.0f, 0.0f, 1.0f));
    shape.setGeometry(tri);
    objRoot.addChild(shape);

    return objRoot;
  }

  /**
   * This is our entrypoint to the application.  This code is not called when the program runs as an applet.
   *
   * @param args - command line arguments (unused)
   */
  public static void main(String args[])
  {
    // MainFrame allows an applet to run as an application
    Frame frame = new MainFrame(new TriColor(), 320, 280);
    // Put the title in the application titlebar.  The titlebar
    // isn't visible when running as an applet.
    frame.setTitle("A Colorful Triangle");
  }
}

