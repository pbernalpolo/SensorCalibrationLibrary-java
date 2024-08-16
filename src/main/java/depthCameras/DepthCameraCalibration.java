package depthCameras;


import java.util.ArrayList;
import java.util.List;

import numericalLibrary.types.Matrix;
import numericalLibrary.types.Vector3;



/**
 * Represents the calibration of a depth camera.
 */
public interface DepthCameraCalibration
{
    ////////////////////////////////////////////////////////////////
    // PUBLIC ABSTRACT METHODS
    ////////////////////////////////////////////////////////////////
    
    /**
     * Returns the 3-dimensional position of the point projected into the pixel (i,j).
     * 
     * @param i     x-coordinate of the pixel.
     * @param j     y-coordinate of the pixel.
     * @param depthValue    depth value measured by the depth camera [m].
     * 
     * @return  3-dimensional position of the point projected into the pixel (i,j).
     */
    public Vector3 pixelToPoint3( double i , double j , double depthValue );
    
    
    /**
     * Returns the number of optimizable parameters used in the calibration model.
     */
    public int numberOfParameters();
    
    
    /**
     * Sets the optimizable parameters used in the calibration model.
     * 
     * @param theta     column matrix that contains the coptimizable parameters to be set.
     */
    public void setParameters( Matrix theta );
    
    
    /**
     * Returns the optimizable parameters used in the calibration model stored in a column matrix.
     * 
     * @return  optimizable parameters used in the calibration model stored in a column matrix.
     */
    public Matrix getParameters();
    
    
    
    ////////////////////////////////////////////////////////////////
    // PUBLIC DEFAULT METHODS
    ////////////////////////////////////////////////////////////////
    
    /**
     * Returns the point cloud that results from the raw depth image.
     * 
     * @param image     raw depth image.
     * @return  point cloud that results from the raw depth image.
     */
    public default List<Vector3> imageToPointCloud( DepthImage image )
    {
        List<Vector3> pointCloud = new ArrayList<Vector3>();
        for( int i=0; i<image.rows(); i++ ) {
            for( int j=0; j<image.cols(); j++ ) {
                if( image.checkPixel( i , j ) ) {
                    // Take pixel.
                    double depthValue = image.depth( i , j );
                    // Build the RealVector3 from the (i,j)-th index and the depth value.
                    Vector3 point = this.pixelToPoint3( i , j , depthValue );
                    // Add the point to the point cloud.
                    pointCloud.add( point );
                }
            }
        }
        return pointCloud;
    }
    
}
