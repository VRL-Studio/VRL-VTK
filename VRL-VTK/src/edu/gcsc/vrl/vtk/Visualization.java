/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import vtk.vtkActor;
import vtk.vtkActor2D;
import vtk.vtkRenderer;
/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class Visualization {
    private Collection<vtkActor> actors = new ArrayList<vtkActor>();
    private Collection<vtkActor2D> actors2D = new ArrayList<vtkActor2D>();
    
    private Color background = new Color(120,120,120);

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
    
    public boolean removeActor(vtkActor a) {
        return actors.remove(a);
    }
    
    public boolean removeActor2D(vtkActor2D a) {
        return actors2D.remove(a);
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
    
    void registerWithRenderer(vtkRenderer renderer) {
        for (vtkActor actor : actors) {
            renderer.AddActor(actor);
        }
        
        for (vtkActor2D actor : actors2D) {
            renderer.AddActor2D(actor);
        }
    }
    
    void unregisterFromRenderer(vtkRenderer renderer) {
        for (vtkActor actor : actors) {
            renderer.RemoveActor(actor);
        }
        
        for (vtkActor2D actor : actors2D) {
            renderer.RemoveActor2D(actor);
        }
    }

}
