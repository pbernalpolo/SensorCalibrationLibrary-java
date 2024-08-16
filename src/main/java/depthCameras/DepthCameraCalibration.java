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
    
    public Vector3 pixelToPoint3( double i , double j , double depthValue );
    
    public int numberOfParameters();
    
    public void setParameters( Matrix theta );
    
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
