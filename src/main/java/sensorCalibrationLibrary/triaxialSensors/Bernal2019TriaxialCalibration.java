package sensorCalibrationLibrary.triaxialSensors;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;



/**
 * Implements the triaxial calibration described in:
 * <a href>https://ieeexplore.ieee.org/abstract/document/8861358</a>
 */
public class Bernal2019TriaxialCalibration
    implements TriaxialCalibration
{
    ////////////////////////////////////////////////////////////////
    // PRIVATE VARIABLES
    ////////////////////////////////////////////////////////////////
    
    /**
     * Order of the polynomial.
     */
    private int N;
    
    /**
     * 9 * {@link #N}.
     * This variable is defined to increase efficiency.
     */
    private int N9;
    
    /**
     * Temperature used to correct measurements.
     */
    private double temperature;
    
    private double temperatureMin;
    private double temperatureMax;
    
    /**
     * Calibration coefficients.
     */
    private double[] z;
    
    
    
    ////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////
    
    public void set( double[] calibrationCoefficients )
    {
        this.reset( calibrationCoefficients.length/9-1 );
        System.arraycopy( calibrationCoefficients , 0 ,  this.z , 0 , calibrationCoefficients.length );
    }
    
    
    public double[] correct( double[] input )
    {
      double[] output = new double[3];
      double Tn = 1.0;
      for(int n9=0; n9<=this.N9; n9+=9) {
          output[0] += ( this.z[n9+0] * input[0]                                                          +  this.z[n9+6] ) * Tn;
          output[1] += ( this.z[n9+1] * input[0]  +  this.z[n9+2] * input[1]                              +  this.z[n9+7] ) * Tn;
          output[2] += ( this.z[n9+3] * input[0]  +  this.z[n9+4] * input[1]  +  this.z[n9+5] * input[2]  +  this.z[n9+8] ) * Tn;
          Tn *= this.temperature;
      }
      return output;
    }
    
    
    public void save( String path )
    {
        try {
            // Create calibration file.
            BufferedWriter writer = new BufferedWriter( new FileWriter( path ) );
            // Store the order of the polynomial used for calibration, and the temperature range.
            writer.write( this.N + " " + this.temperatureMin + " " + this.temperatureMax + "\n" );
            // Store calibration.
            for( int n=0; n<=this.N; n++ ) {
                for( int i=0; i<9; i++ ) {
                    writer.write( " " + this.z[n*9+i] );
                }
                writer.write( "\n" );
            }
            // we store the variance
            /*calibrationFile.println( this.z[0]*this.z[0]*this.varX + " " + this.z[2]*this.z[2]*this.varY + " " + this.z[5]*this.z[5]*this.varZ );
            calibrationFile.println( "\n\n\n" );*/
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
            // Obtain the order of the polynomial
            String strLine = br.readLine();
            String[] strValues = strLine.split(" ");
            int polynomialOrder = Integer.parseInt( strValues[0] );
            this.temperatureMin = Double.parseDouble( strValues[1] );
            this.temperatureMax = Double.parseDouble( strValues[2] );
            this.reset( polynomialOrder );
            // Obtain the matrix elements and the offset for each degree.
            for( int n=0; n<=polynomialOrder; n++ ) {
                strLine = br.readLine();
                strValues = strLine.split(" ");
                double[] theValues = new double[0];
                for( int i=0; i<strValues.length; i++ ) {
                    try {
                        double newDouble = Double.parseDouble( strValues[i] );
                        double[] newValues = new double[theValues.length+1];
                        for(int j=0; j<theValues.length; j++) newValues[j] = theValues[j];
                        newValues[ theValues.length ] = newDouble;
                        theValues = newValues;
                    }catch( NumberFormatException e ){
                    }
                }
                if( theValues.length != 9 ) throw new Exception( "TriaxialCalibration: wrong calibration." );
                for(int i=0; i<9; i++) {
                    this.z[n*9+i] = theValues[i];
                }
            }
            // Close.
            br.close();
            fstream.close();
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }
    
    
    
    ////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////
    
    private void reset( int polynomialOrder )
    {
        this.N = polynomialOrder;
        this.N9 = 9 * polynomialOrder;
        this.z = new double[9*(polynomialOrder+1)];
    }
    
}
