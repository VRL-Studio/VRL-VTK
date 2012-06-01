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
import vtk.vtkRenderer;
/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class Visualization {
    private Collection<vtkActor> actors = new ArrayList<vtkActor>();
    
    private Color background = new Color(120,120,120);

    public Visualization(vtkActor... actors) {
        this.actors.addAll(Arrays.asList(actors));
    }
    
        
    public void addActor(vtkActor a) {
        actors.add(a);
    }
    
    public boolean removeActor(vtkActor a) {
        return actors.remove(a);
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
    }
    
    void unregisterFromRenderer(vtkRenderer renderer) {
        for (vtkActor actor : actors) {
            renderer.RemoveActor(actor);
        }
    }

}
