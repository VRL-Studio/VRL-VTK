/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.annotation.TypeInfo;
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
import javax.media.j3d.Transform3D;
import javax.swing.JMenuItem;
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

//        addAxes();
//        addScalarBar();
//        addTitle("Plot");
        addMenu();
    }

    private void addAxes() {
        vtkAxesActor axesActor = new vtkAxesActor();
        axesActor.AxisLabelsOn();
        axesActor.SetShaftTypeToCylinder();
        axesActor.SetCylinderRadius(0.05);
        axesActor.SetConeRadius(0.5);
        axesActor.SetNormalizedTipLength(0.3, 0.3, 0.3);
        axesActor.SetConeResolution(32);
        axesActor.SetAxisLabels(0);
        vtkOrientationMarkerWidget axesOrientation = new vtkOrientationMarkerWidget();
        axesOrientation.SetOrientationMarker(axesActor);
        axesOrientation.SetInteractor(view.getPanel().getRenderWindowInteractor());
        axesOrientation.InteractiveOff();
        axesOrientation.SetViewport(0, 0, 0.25, 0.25);
        axesOrientation.SetOutlineColor(1.0, 1.0, 1.0);
        axesOrientation.SetEnabled(1);
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
        scalarBarWidget = new vtkScalarBarWidget();

        scalarBarWidget.SetInteractor(view.getPanel().getRenderWindowInteractor());
        scalarBarWidget.SetScalarBarActor(scalarBar);
        scalarBarWidget.SetEnabled(0);
    }

    private void addTitle(String title) {
        vtkTextActor textActor = new vtkTextActor();
        textActor.SetInput(title);

        textActor.GetTextProperty().SetColor(1, 1, 1);
//        textActor.GetTextProperty().BoldOn();
//        textActor.GetTextProperty().SetFontFamilyToArial();
//        textActor.GetTextProperty().ShadowOn();
        textActor.GetTextProperty().SetJustificationToCentered();

        vtkTextRepresentation textRepresentation =
                new vtkTextRepresentation();

        textRepresentation.GetPositionCoordinate().SetValue(0.3, 0.80);
        textRepresentation.GetPosition2Coordinate().SetValue(0.4, 0.15);

        titleWidget = new vtkTextWidget();
        titleWidget.SetInteractor(view.getPanel().getRenderWindowInteractor());
        titleWidget.SetRepresentation(textRepresentation);

        titleWidget.SetTextActor(textActor);
        titleWidget.SetSelectable(0);
        titleWidget.SetEnabled(1);
    }

    private void addMenu() {
        JMenuItem resetItem = new JMenuItem("Reset View");
        resetItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                view.resetCamera();
            }
        });

        view.getMenu().add(resetItem);

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
                if (!(o instanceof Visualization)) {
                    return;
                }

                final Visualization v = (Visualization) o;
                
                view.getPanel().lock();
                
                v.registerWithRenderer(view.getRenderer());

//                if (v.getLookupTable() != null) {
//                    scalarBar.SetLookupTable(v.getLookupTable());
//                    scalarBarWidget.SetEnabled(1);
//
//                    if (v.getValueTitle() != null) {
//                        scalarBar.SetTitle(v.getValueTitle());
//                    }
//
//                    if (v.getTitle() != null) {
//                        addTitle(v.getTitle());
//                    }
//
//                } else {
//                    scalarBarWidget.SetEnabled(0);
//                }

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

//                scalarBarWidget.SetEnabled(0);
//                titleWidget.SetEnabled(0);

                view.contentChanged();
                view.deleteContent();
                view.repaint();
            }
        });

        viewValue = null;
    }

    /**
     * Defines the Vcanvas3D size by evaluating a groovy script.
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

        System.out.println(getValueOptions());
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
