/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.annotation.TypeInfo;
import eu.mihosoft.vrl.dialogs.FileDialogManager;
import eu.mihosoft.vrl.io.FileSaver;
import eu.mihosoft.vrl.io.ImageFilter;
import eu.mihosoft.vrl.io.ImageSaver;
import eu.mihosoft.vrl.reflection.CustomParamData;
import eu.mihosoft.vrl.reflection.TypeRepresentationBase;
import eu.mihosoft.vrl.visual.ResizableContainer;
import eu.mihosoft.vrl.visual.VContainer;
import eu.mihosoft.vrl.visual.VGraphicsUtil;
import eu.mihosoft.vrl.visual.VSwingUtil;
import eu.mihosoft.vtk.VTKJPanel;
import groovy.lang.Script;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import javax.media.j3d.Transform3D;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;
import vtk.*;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
@TypeInfo(type = Visualization.class, input = false, output = true, style = "default")
public class VTKOutputType extends TypeRepresentationBase {

    private Visualization viewValue;
//    private VTKView view;
    private VTKCanvas3D view;
//    private JFrame frame;
    private static int NUMBER_OF_INSTANCES;
    private static int MAX_NUMBER_OF_INSTANCES = 16;
    public static final String POSITION_KEY = "position";
    public static final String FOCAL_POINT_KEY = "focal-point";
    public static final String ROLL_KEY = "roll";
    private vtkScalarBarActor scalarBar;
    private vtkScalarBarWidget scalarBarWidget;
    private vtkTextWidget titleWidget;
    private vtkOrientationMarkerWidget axesWidget;

    public VTKOutputType() {

        NUMBER_OF_INSTANCES++;

        if (NUMBER_OF_INSTANCES > MAX_NUMBER_OF_INSTANCES) {
            NUMBER_OF_INSTANCES--;
            throw new IllegalStateException(
                    getClass()
                    + " only supports up to "
                    + MAX_NUMBER_OF_INSTANCES
                    + " number of instances!");
        }

        VSwingUtil.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                initialize();
            }
        });
    }

    private void initialize() {

        view = new VTKCanvas3D(this);

        view.setMinimumSize(new Dimension(160, 120));
        view.setMaximumSize(new Dimension(600, 600));
        view.getRenderer().GetActiveCamera().Dolly(0.2);
        view.setOpaque(false);

        VContainer cont = new VContainer();
        cont.add(view);

        add(cont);

        cont.setMinimumSize(new Dimension(160, 120));
        cont.setPreferredSize(new Dimension(160, 120));
        cont.setMaximumSize(new Dimension(600, 600));

        setValueOptions("width=160;height=120;blurValue=0.7F;");

        this.setInputComponent(cont);

        addAxes();
        addScalarBar();
        addTitle("Plot");
        addMenu();
    }

    private void addAxes() {

        vtkAxesActor axesActor = new vtkAxesActor();
        axesActor.SetShaftTypeToCylinder();
        axesActor.SetCylinderRadius(0.05);
        axesActor.SetConeRadius(0.5);
        axesActor.SetNormalizedTipLength(0.3, 0.3, 0.3);
        axesActor.SetConeResolution(32);
        axesActor.SetAxisLabels(1);
        axesActor.SetXAxisLabelText("X");
        axesActor.SetYAxisLabelText("Y");
        axesActor.SetZAxisLabelText("Z");
        // we need global reference to widgets that are not explicitly
        // added to renderer. otherwise the VTK GC will delete related memory.
        // This is a VTK bug.
        axesWidget = new vtkOrientationMarkerWidget();
        axesWidget.SetOrientationMarker(axesActor);
        view.getPanel().lock();
        axesWidget.SetInteractor(view.getPanel().getRenderWindowInteractor());
        axesWidget.InteractiveOff();
        axesWidget.SetViewport(0, 0, 0.25, 0.25);
        axesWidget.SetOutlineColor(1.0, 1.0, 1.0);
        axesWidget.SetEnabled(1);
        view.getPanel().unlock();
    }

    private void addScalarBar() {
        scalarBar = new vtkScalarBarActor();
        scalarBar.SetNumberOfLabels(4);

        vtkLookupTable hueLut = new vtkLookupTable();
        hueLut.SetTableRange(0, 1);
        hueLut.SetHueRange(0, 1);
        hueLut.SetSaturationRange(1, 1);
        hueLut.SetValueRange(1, 1);
        hueLut.Build();

        scalarBar.SetLookupTable(hueLut);

        // we need global reference to widgets that are not explicitly
        // added to renderer. otherwise the VTK GC will delete related memory.
        // This is a VTK bug.
        scalarBarWidget = new vtkScalarBarWidget();

        scalarBarWidget.SetInteractor(view.getPanel().getRenderWindowInteractor());
        scalarBarWidget.SetScalarBarActor(scalarBar);
        scalarBarWidget.SetEnabled(0);
    }

    private void addTitle(String title) {
        vtkTextActor textActor = new vtkTextActor();
        textActor.SetInput(title);

        textActor.GetTextProperty().SetColor(1, 1, 1);
        textActor.GetTextProperty().SetJustificationToCentered();

        vtkTextRepresentation textRepresentation =
                new vtkTextRepresentation();

        textRepresentation.GetPositionCoordinate().SetValue(0.3, 0.80);
        textRepresentation.GetPosition2Coordinate().SetValue(0.4, 0.15);

        // we need global reference to widgets that are not explicitly
        // added to renderer. otherwise the VTK GC will delete related memory.
        // This is a VTK bug.
        titleWidget = new vtkTextWidget();
        titleWidget.SetInteractor(view.getPanel().getRenderWindowInteractor());
        titleWidget.SetRepresentation(textRepresentation);

        titleWidget.SetTextActor(textActor);
        titleWidget.SetSelectable(0);
        titleWidget.SetEnabled(1);
    }

    private void saveImage() {
        FileDialogManager dialogManager = new FileDialogManager();

        class DummySaver implements FileSaver {

            File imgFile;

            public void saveFile(Object o, File file, String ext) throws IOException {
                this.imgFile = file;
            }

            public String getDefaultExtension() {
                return "png";
            }
        }

        class PNGFilter extends FileFilter {

            @Override
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".png")
                        || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Image Files (png)";
            }
        }

        DummySaver imgSaver = new DummySaver();
        dialogManager.saveFile(this, new Object(), imgSaver, new PNGFilter());

        if (imgSaver.imgFile != null) {
            view.getPanel().lock();

            vtkWindowToImageFilter w2if = new vtkWindowToImageFilter();
            w2if.SetInput(view.getPanel().GetRenderWindow());

            w2if.SetMagnification(3); // should we specify this in save dialog?
            w2if.Update();

            vtkPNGWriter writer = new vtkPNGWriter();
            writer.SetInput(w2if.GetOutput());
            writer.SetFileName(imgSaver.imgFile.getAbsolutePath());
            writer.Write();

            view.getPanel().unlock();
        }
    }

    private void addMenu() {
        JMenuItem resetItem = new JMenuItem("Reset View");
        resetItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                view.resetCamera();
            }
        });

        view.getMenu().add(resetItem);

        JMenuItem imgItem = new JMenuItem("Save Image");
        imgItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveImage();
            }
        });

        view.getMenu().add(imgItem);
        
        final String perspToOrtho = "Toggle Projection (Persp. -> Ortho)";
        final String orthoToPersp = "Toggle Projection (Ortho. -> Persp.)";
        
        final JMenuItem toggleProjectionItem = new JMenuItem(perspToOrtho);
        toggleProjectionItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                view.setParallelProjection(!view.isParallelProjection());
                if (view.isParallelProjection()) {
                    toggleProjectionItem.setText(orthoToPersp);
                } else {
                    toggleProjectionItem.setText(perspToOrtho);
                }
            }
        });

        view.getMenu().add(toggleProjectionItem);

        view.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON3 && view.getMenu() != null) {
                    view.getMenu().show(view, e.getX(), e.getY());
                }
            }
        });
    }

    @Override
    public void setViewValue(final Object o) {

        VSwingUtil.invokeAndWait(new Runnable() {

            public void run() {

                // check if type is correct
                if (!(o instanceof Visualization)) {
                    return;
                }

                final Visualization v = (Visualization) o;

                // lock panel (may crash otherwise)
                view.getPanel().lock();

                // add actors and volumes
                v.registerWithRenderer(view.getRenderer());

                // check whether scalarbar shall be shown
                if (v.getLookupTable() != null) {
                    scalarBar.SetLookupTable(v.getLookupTable());
                    scalarBarWidget.SetEnabled(1);

                    if (v.getValueTitle() != null) {
                        scalarBar.SetTitle(v.getValueTitle());
                    }

                } else {
                    scalarBarWidget.SetEnabled(0);
                }

                // show title if avalable
                if (v.getTitle() != null) {
                    addTitle(v.getTitle());
                }

                // enable orientation widget if requested
                if (v.isOrientationVisible()) {
                    axesWidget.SetEnabled(1);
                } else {
                    axesWidget.SetEnabled(0);
                }

                viewValue = v;

                view.setBackground(v.getBackground());
                view.contentChanged();
                view.repaint();

                view.getPanel().unlock();
            }
        });
    }

    @Override
    public Object getViewValue() {
        return viewValue;
    }

    @Override
    public void emptyView() {

        super.emptyView();

        VSwingUtil.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (viewValue != null) {
                    viewValue.unregisterFromRenderer(view.getRenderer());
//                    viewValue.dispose();
                }

                scalarBarWidget.SetEnabled(0);
                titleWidget.SetEnabled(0);

                view.contentChanged();
                view.deleteContent();
                view.repaint();
            }
        });

        viewValue = null;
    }

    /**
     * Defines the VTKCanvas3D size by evaluating a groovy script.
     *
     * @param script the script to evaluate
     */
    private void setVCanvas3DSizeFromValueOptions(Script script) {
        Integer w = null;
        Integer h = null;
        Object property = null;

        if (getValueOptions() != null) {

            if (getValueOptions().contains("width")) {
                property = script.getProperty("width");
            }

            if (property != null) {
                w = (Integer) property;
            }

            property = null;

            if (getValueOptions().contains("height")) {
                property = script.getProperty("height");
            }

            if (property != null) {
                h = (Integer) property;
            }
        }

        if (w != null && h != null) {
            // TODO find out why offset is 10
            view.setPreferredSize(new Dimension(w - 10, h));
            view.setSize(new Dimension(w - 10, h));
        }

        // System.out.println(getValueOptions());
    }

    @Override
    protected void evaluationRequest(Script script) {
        setVCanvas3DSizeFromValueOptions(script);
    }

    @Override
    public CustomParamData getCustomData() {

        CustomParamData result = super.getCustomData();

        double[] pos = view.getRenderer().GetActiveCamera().GetPosition();
        double[] focal = view.getRenderer().GetActiveCamera().GetFocalPoint();
        double roll = view.getRenderer().GetActiveCamera().GetRoll();

        result.put(POSITION_KEY, pos);
        result.put(FOCAL_POINT_KEY, focal);
        result.put(ROLL_KEY, roll);

        return result;
    }

    @Override
    public void evaluateCustomParamData() {

        double[] pos = (double[]) super.getCustomData().get(POSITION_KEY);
        double[] focal = (double[]) super.getCustomData().get(FOCAL_POINT_KEY);
        Double roll = (Double) super.getCustomData().get(ROLL_KEY);

        if (pos != null) {
            view.getRenderer().GetActiveCamera().SetPosition(pos);
        }

        if (focal != null) {
            view.getRenderer().GetActiveCamera().SetFocalPoint(focal);
        }

        if (roll != null) {
            view.getRenderer().GetActiveCamera().SetRoll(roll);
        }

        view.contentChanged();
        view.repaint();
    }

    @Override
    public void dispose() {

        super.dispose();

        if (view != null) {
            view.dispose();
            view = null;
            NUMBER_OF_INSTANCES--;
        }
    }
}
