/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.types.observe.FileAnalyzer;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import vtk.vtkUnstructuredGrid;
import vtk.vtkXMLUnstructuredGridReader;

/**
 *
 * @author Christian Poliwoda <christian.poliwoda@gcsc.uni-frankfurt.de>
 */
class VTUAnalyzer implements FileAnalyzer {

    private String ending = "vtu";
    private String startsWith = "";

    public VTUAnalyzer() {
    }

    /**
     * If file is a dir the first file with the correct ending is choosen, else
     * the file itself.
     *
     * @param file that should be analysed
     *
     * @return the
     */
    public List<String> analyzeFile(File file) {
        //delete previous entries resp. new list
        List<String> fileEntries = new ArrayList<String>();

        ArrayList<File> files = getAllFilesInFolder(file, getStartsWith());

        if (files != null && !files.isEmpty()) {

            File lastFile = files.get(0);
            final String fileName = lastFile.getAbsolutePath();

            vtkXMLUnstructuredGridReader reader = new vtkXMLUnstructuredGridReader();
            reader.SetFileName(fileName);
            reader.Update();

            vtkUnstructuredGrid ug = reader.GetOutput();

//            System.out.println("POINT DATA IN FILE:");
            int numPointData = ug.GetPointData().GetNumberOfArrays();
            for (int i = 0; i < numPointData; i++) {
                fileEntries.add(ug.GetPointData().GetArrayName(i));
//                System.out.println("" + i + ": " + ug.GetPointData().GetArrayName(i));
            }

//            System.out.println("CELL DATA IN FILE:");
            int numCellData = ug.GetCellData().GetNumberOfArrays();
            for (int i = 0; i < numCellData; i++) {
                fileEntries.add(ug.GetCellData().GetArrayName(i));
//                System.out.println("" + i + ": " + ug.GetCellData().GetArrayName(i));
            }

        }

        return fileEntries;
    }

    private ArrayList<File> getAllFilesInFolder(File dir, final String startsWith) {

        ArrayList<File> result = new ArrayList<File>();

        if (dir != null && dir.isDirectory()) {

            for (File f : dir.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathName) {
                    boolean fileAccept = pathName.getName().toLowerCase().endsWith("." + ending) || pathName.isDirectory();

                    int dot = pathName.getPath().lastIndexOf(".");
                    int sep = pathName.getPath().lastIndexOf(File.separator);

                    String fileName = pathName.getPath().substring(sep + 1, dot);

                    boolean nameAccept = startsWith.equals("") || fileName.startsWith(startsWith + "_");

                    return fileAccept && nameAccept;
                }
            })) {
                if (f.isFile()) {
                    result.add(f);
                }
            }

        } else if (dir != null && dir.isFile()) {
            result.add(dir);
        }

        return result;
    }

    /**
     * @return the startsWith
     */
    public String getStartsWith() {
        return startsWith;
    }

    /**
     * @param startsWith the startsWith to set
     */
    public void setStartsWith(String startsWith) {
        this.startsWith = startsWith;
    }
}
