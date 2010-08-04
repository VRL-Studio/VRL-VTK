#include <vtkSmartPointer.h>
#include <vtkXMLUnstructuredGridReader.h>
#include <vtkUnstructuredGrid.h>
#include <vtkCell.h>
#include <vtkIdList.h>
#include <vtkUnsignedCharArray.h>
#include <vtkPointData.h>
#include <string>
 
int main(int argc, char *argv[])
{
  //parse command line arguments
  if(argc != 2)
    {
    std::cerr << "Usage: " << argv[0]
              << " Filename(.vtu)" << std::endl;
    return EXIT_FAILURE;
    }
 
  std::string filename = argv[1];
 
  //read all the data from the file
  vtkSmartPointer<vtkXMLUnstructuredGridReader> reader =
    vtkSmartPointer<vtkXMLUnstructuredGridReader>::New();
  reader->SetFileName(filename.c_str());
  reader->Update();
  vtkUnstructuredGrid* unstructuredGrid = reader->GetOutput();



  
  vtkIdType numCells = unstructuredGrid->GetNumberOfCells();
 
  //get points
  vtkIdType numPoints = unstructuredGrid->GetNumberOfPoints();
  //std::cout << "There are " << numPoints << " points." << std::endl;
  std::cout <<  numPoints << " " << numCells << std::endl;
 
  if(!(numPoints > 0) )
    {
    return EXIT_FAILURE;
    }
 
  double point[3];
 
 for(vtkIdType i = 0; i < numPoints; i++)
    {
    unstructuredGrid->GetPoint(i, point);
    //std::cout << "Point " << i << ": " << point[0] << " " << point[1] << " " << point[2] << std::endl;
    std::cout  << i << " " << point[0] << " " << point[1] << " " << point[2] << std::endl;
    }


/*******************************/


std::cout << "COLORS:" << std::endl;

vtkUnstructuredGrid* unstructuredGrid2 = reader->GetOutput();

vtkIdType numCells2 = unstructuredGrid2->GetNumberOfCells();
 
  //get points
  vtkIdType numPoints2 = unstructuredGrid2->GetNumberOfPoints();
  //std::cout << "There are " << numPoints << " points." << std::endl;
  std::cout <<  numPoints2 << " " << numCells2 << std::endl;
 
 if(!(numPoints2 > 0) )
 {
    return EXIT_FAILURE;
 }
 
  double point2[1];
 

 for(vtkIdType i = 0; i < numPoints2; i++)
 {
    unstructuredGrid2->GetPoint(i, point2);
    //std::cout << "Point " << i << ": " << point[0] << " " << point[1] << " " << point[2] << std::endl;
    std::cout  << i << " " << point2[0] << std::endl;
 }
 

/*******************************/







  //std::cout <<  "TRIANGLES:" << std::endl;
  //get triangles
  //if( (ug->GetCellType(0) == VTK_TRIANGLE) && (NumCells > 0) )//vtkCellType.h
  //vtkIdType numCells = unstructuredGrid->GetNumberOfCells();
  // std::cout << "There are " << numCells << " cells." << std::endl;
 
/* 
 for(vtkIdType tri = 0; tri < numCells; tri++)
    {
    vtkSmartPointer<vtkCell> cell = unstructuredGrid->GetCell(tri);
    vtkSmartPointer<vtkIdList> pts = cell->GetPointIds();
    std::vector<int> list(3);
    list[0] = pts->GetId(0);
    list[1] = pts->GetId(1);
    list[2] = pts->GetId(2);

	std::cout << tri << " " << list[0] << " " << list[1] << " " << list[2] << std::endl;

    }
 */
/* DOES NOT WORK ? 
 //get colors
  vtkSmartPointer<vtkUnsignedCharArray> colorsData = vtkUnsignedCharArray::SafeDownCast(
    unstructuredGrid->GetPointData()->GetArray("Colors"));
 
if(colorsData)
    { 
    unsigned char color[3];
    for(unsigned int i = 0; i < static_cast<unsigned int> (numPoints); i++)
      {
      colorsData->GetTupleValue(i, color);
      std::cout << "Color " << i << ": "
                << static_cast<int>(color[0]) << " "
                << static_cast<int>(color[1]) << " "
                << static_cast<int>(color[2]) << std::endl;
      }
   }*/
  return EXIT_SUCCESS;
}

