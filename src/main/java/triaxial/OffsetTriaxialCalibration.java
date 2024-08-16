package triaxial;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;



/**
 * Implements the triaxial calibration in which an offset is applied.
 */
public class OffsetTriaxialCalibration
    implements TriaxialCalibration
{
    ////////////////////////////////////////////////////////////////
    // PRIVATE VARIABLES
    ////////////////////////////////////////////////////////////////
    
    /**
     * Calibration offsets.
     */
    private final double[] offset;
    
    
    
    ////////////////////////////////////////////////////////////////
    // PUBLIC CONSTRUCTORS
    ////////////////////////////////////////////////////////////////
    
    public OffsetTriaxialCalibration()
    {
        this.offset = new double[3];
    }
    
    
    
    ////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////
    
    public void setOffset( double offsetX , double offsetY , double offsetZ )
    {
        this.offset[0] = offsetX;
        this.offset[1] = offsetY;
        this.offset[2] = offsetZ;
    }
    
    
    public double[] correct( double[] input )
    {
      double[] output = new double[3];
      output[0] = input[0] + this.offset[0];
      output[1] = input[1] + this.offset[1];
      output[2] = input[2] + this.offset[2];
      return output;
    }
    
    
    public void save( String path )
    {
        try {
            // Create calibration file.
            BufferedWriter writer = new BufferedWriter( new FileWriter( path ) );
            // Store offsets.
            writer.write( this.offset[0] + "\n" );
            writer.write( this.offset[1] + "\n" );
            writer.write( this.offset[2] + "\n" );
            writer.flush();
            writer.close();
        } catch( IOException e ) {
            e.printStackTrace();
        }
    }
    
    
    public void load( String path )
    {
        try {
            // Open the file.
            FileInputStream fstream = new FileInputStream( path );
            BufferedReader br = new BufferedReader( new InputStreamReader(fstream) );
            // Obtain offsets.
            for( int i=0; i<3; i++ ) {
                String strLine = br.readLine();
                this.offset[i] = Double.parseDouble( strLine );
            }
            // Close.
            br.close();
            fstream.close();
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }
    
}
