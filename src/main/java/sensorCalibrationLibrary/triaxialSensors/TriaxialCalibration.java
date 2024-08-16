package sensorCalibrationLibrary.triaxialSensors;



public interface TriaxialCalibration
{
    ////////////////////////////////////////////////////////////////
    // PUBLIC ABSTRACT METHODS
    ////////////////////////////////////////////////////////////////
    
    public double[] correct( double[] input );
    
    public void save( String path );
    
    public void load( String path );
    
}
