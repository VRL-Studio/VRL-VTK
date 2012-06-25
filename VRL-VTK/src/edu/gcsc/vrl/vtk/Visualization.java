/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.visual.VSwingUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import vtk.*;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class Visualization {

    private transient Collection<vtkActor> actors = new ArrayList<vtkActor>();
    private transient Collection<vtkVolume> volumes = new ArrayList<vtkVolume>();
    private transient Collection<vtkActor2D> actors2D = new ArrayList<vtkActor2D>();
    private transient Color background = new Color(120, 120, 120);
    private transient vtkLookupTable lookupTable;
    private transient String valueTitle;
    private transient String title;
    private boolean orientationVisible = true;

    public Visualization(vtkActor... actors) {
        this.actors.addAll(Arrays.asList(actors));
    }

    public void addActor(vtkActor a) {
        actors.add(a);
    }

    public void addActors(vtkActor... a) {
        actors.addAll(Arrays.asList(a));
    }

    public void addActor2D(vtkActor2D a) {
        actors2D.add(a);
    }

    public void addActors2D(vtkActor2D... a) {
        actors2D.addAll(Arrays.asList(a));
    }
    
    public void addVolume(vtkVolume v) {
        volumes.add(v);
    }
    
    public void addVolumes(vtkVolume... v) {
        volumes.addAll(Arrays.asList(v));
    }

    public boolean removeActor(vtkActor a) {
        return actors.remove(a);
    }

    public boolean removeActor2D(vtkActor2D a) {
        return actors2D.remove(a);
    }
    
    public boolean removeVolume(vtkVolume v) {
        return volumes.remove(v);
    }

    /**
     * @return the background
     */
    public Color getBackground() {
        return background;
    }

    /**
     * @param background the background to set
     */
    public void setBackground(Color background) {
        this.background = background;
    }

    void registerWithRenderer(final vtkRenderer renderer) {

        // ensure we execute this on EDT (ui thread)
        VSwingUtil.invokeLater(new Runnable() {

            public void run() {
                for (vtkActor actor : actors) {
                    renderer.AddActor(actor);
                }

                for (vtkActor2D actor : actors2D) {
                    renderer.AddActor2D(actor);
                }
                
                for (vtkVolume volume : volumes) {
                    renderer.AddVolume(volume);
                }
            }
        });
    }

    void unregisterFromRenderer(final vtkRenderer renderer) {

        // ensure we execute this on EDT (ui thread)
        VSwingUtil.invokeLater(new Runnable() {

            public void run() {
                for (vtkActor actor : actors) {
                    renderer.RemoveActor(actor);
                }

                for (vtkActor2D actor : actors2D) {
                    renderer.RemoveActor2D(actor);
                }
                
                for (vtkVolume volume : volumes) {
                    renderer.RemoveVolume(volume);
                }
            }
        });
    }

    /**
     * @return the lookupTable
     */
    public vtkLookupTable getLookupTable() {
        return lookupTable;
    }

    /**
     * @param lookupTable the lookupTable to set
     */
    public void setLookupTable(vtkLookupTable lookupTable) {
        this.lookupTable = lookupTable;
    }

    /**
     * @return the valueTitle
     */
    public String getValueTitle() {
        return valueTitle;
    }

    /**
     * @param valueTitle the valueTitle to set
     */
    public void setValueTitle(String valueTitle) {
        this.valueTitle = valueTitle;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

// We currently use VTK GC
//    void dispose() {
//        for (vtkActor actor : actors) {
//            actor.Delete();
//        }
//
//        for (vtkActor2D actor : actors2D) {
//            actor.Delete();
//        }
//
//        lookupTable.Delete();
//    }

    /**
     * @return the orientationVisible
     */
    public boolean isOrientationVisible() {
        return orientationVisible;
    }

    /**
     * @param orientationVisible the orientationVisible to set
     */
    public void setOrientationVisible(boolean orientationVisible) {
        this.orientationVisible = orientationVisible;
    }
}
