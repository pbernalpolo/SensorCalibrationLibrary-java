package depthCameras.maixSenseA010;


import depthCameras.DepthCameraCalibration;
import numericalLibrary.types.Matrix;
import numericalLibrary.types.Vector3;



/**
 * Implements the default calibration provided in the MaixSense-A010 ToF camera datasheet.
 * 
 * @see <a href>https://www.marutsu.co.jp/contents/shop/marutsu/datasheet/switchscience_DFROBOT-SEN0581.pdf</a>
 */
public class MaixSenseA010DefaultCalibration
    implements DepthCameraCalibration
{
    ////////////////////////////////////////////////////////////////
    // PRIVATE CONSTANTS
    ////////////////////////////////////////////////////////////////
    
    /**
     * Field of view of the camera in the horizontal direction.
     * <p>
     * Given in [rad].
     * 
     * @see <a href>https://www.marutsu.co.jp/contents/shop/marutsu/datasheet/switchscience_DFROBOT-SEN0581.pdf</a>, table under "Key Specifications" in page 6.
     */
    private static final double FOV_H = 70 * Math.PI/180.0;  // [rad] See table under "Key Specifications" in page 6
    
    /**
     * Field of view of the camera in the vertical direction.
     * <p>
     * Given in [rad].
     * 
     * @see <a href>https://www.marutsu.co.jp/contents/shop/marutsu/datasheet/switchscience_DFROBOT-SEN0581.pdf</a>, table under "Key Specifications" in page 6.
     */
    private static final double FOV_V = 60 * Math.PI/180.0;
    
    private int imageRowsCols;
    
    
    
    ////////////////////////////////////////////////////////////////
    // PRIVATE DERIVED CONSTANTS
    ////////////////////////////////////////////////////////////////
    
    /**
     * Size of the projection screen in the x-direction when such screen is 1 meter apart from the camera focus.
     */
    private static final double X_SCREEN_SIZE_AT_1M = 2.0 * Math.tan( FOV_H / 2.0 );
    
    /**
     * Size of the projection screen in the y-direction when such screen is 1 meter apart from the camera focus.
     */
    private static final double Y_SCREEN_SIZE_AT_1M = 2.0 * Math.tan( FOV_V / 2.0 );
    
    
    
    ////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////
    
    public void setImageSize( int numberOfRowsAndColumns )
    {
        this.imageRowsCols = numberOfRowsAndColumns;
    }
    
    
    public Vector3 pixelToPoint3( double i , double j , double depthValue )
    {
        Vector3 output = new Vector3(
                (j-0.5*imageRowsCols)/imageRowsCols * X_SCREEN_SIZE_AT_1M ,
                (i-0.5*imageRowsCols)/imageRowsCols * Y_SCREEN_SIZE_AT_1M ,
                1.0 );
        output.scaleInplace( depthValue / output.norm() );
        return output;
    }
    
    
    public void setParameters( Matrix theta )
    {
    }


    public int numberOfParameters()
    {
        return 0;
    }


    public Matrix getParameters()
    {
        return null;
    }
    
}
