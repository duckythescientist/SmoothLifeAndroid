package ninja.duck.smoothlife;

import android.graphics.Color;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ninja.duck.smoothlife.ColorMap: Classical linear LUT, with support for non-applicable values (
 * {@link Double#NaN}) and missing values.
 * <p>
 * Associate a color to a scalar number. The color is taken from a linear
 * interpolation within a list of colors at specific positions between 0 and 1.
 * Specific colors are returned for {@link Double#NaN} values and for missing
 * values.
 * <p>
 * A list of colormaps contains defaults plus the colormaps that are
 * deserialized from a YAML file.
 *
 * @author Jean-Yves Tinevez
 *
 * Originally from: https://github.com/tinevez/CircleSkinner/blob/master/src/main/java/net/imagej/circleskinner/util/ColorMap.java
 * Edited by duck
 */
public class ColorMap
{
    final int[] colors;

    final double[] alphas;

    private final int nColors;

    final int missingColor;

    final int notApplicableColor;

    String name;

    /** The Jet colormap. Interpolates from blue to green to red. */
    public static final ColorMap JET;

    /**
     * The cube-helix colormap. Adequate to represent a linear increase in
     * brightness spanning over several colors.
     */
    public static final ColorMap CUBE_HELIX;

    /** The Parula colormap. Monotonic changes in brightness, from blue to yellow. */
    public static final ColorMap PARULA;

    /**
     * The Viridis colormap. Monotonic changes in brightness, from dark purple
     * to yellow.
     */
    public static final ColorMap VIRIDIS;

    /**
     * The Seismic colormap. Good to split two categories. Dark blue on one
     * side, dark red on the other and white in the middle.
     */
    public static final ColorMap SEISMIC;

    public static final ColorMap GRAY;

    public static final ColorMap MAGMA;
    public static final ColorMap INFERNO;
    public static final ColorMap PLASMA;
    public static final ColorMap TWILIGHT;
    public static final ColorMap TURBO;
    public static final ColorMap RAINBOW;

    public static final ColorMap BETTER_RAINBOW;


    private static Map< String, ColorMap > colorMaps = new LinkedHashMap<>();
    static
    {
        JET = ColorMap.jet();
        colorMaps.put( JET.name, JET );
        CUBE_HELIX = ColorMap.cubeHelix();
        colorMaps.put( CUBE_HELIX.name, CUBE_HELIX );
        PARULA = ColorMap.parula();
        colorMaps.put( PARULA.name, PARULA );
        SEISMIC = ColorMap.seismic();
        colorMaps.put( SEISMIC.name, SEISMIC );
        GRAY = ColorMap.gray();
        colorMaps.put( GRAY.name, GRAY);

        VIRIDIS = ColorMap.viridis();
        colorMaps.put( VIRIDIS.name, VIRIDIS );
        MAGMA = ColorMap.magma();
        colorMaps.put( MAGMA.name, MAGMA);
        INFERNO = ColorMap.inferno();
        colorMaps.put( INFERNO.name, INFERNO);
        PLASMA = ColorMap.plasma();
        colorMaps.put( PLASMA.name, PLASMA);
        TWILIGHT = ColorMap.twilight();
        colorMaps.put( TWILIGHT.name, TWILIGHT);
        TURBO = ColorMap.turbo();
        colorMaps.put( TURBO.name, TURBO);
        RAINBOW = ColorMap.rainbow();
        colorMaps.put( RAINBOW.name, RAINBOW);
        BETTER_RAINBOW = ColorMap.better_rainbow();
        colorMaps.put( BETTER_RAINBOW.name, BETTER_RAINBOW);

    }

    /**
     * Returns the collection of names of colormaps available.
     *
     * @return the colormap names.
     */
    public static Collection< String > getColorMapNames()
    {
        return Collections.unmodifiableCollection( colorMaps.keySet() );
    }

    /**
     * Returns the colormap instance with the specified name. Defaults to the
     * Jet colormap if the specified name is unknown.
     *
     * @param name
     *            the colormap name.
     * @return the colormap instance.
     */
    public static ColorMap getColorMap( final String name )
    {
        final ColorMap cm = colorMaps.get( name.toLowerCase() );
        return cm == null ? JET : cm;
    }

    /**
     * Generates a colormap linearly interpolated from the specified color.
     *
     * @param colors
     *            the color list. It must at least contains 1 element.
     * @param alphas
     *            the position of the colors in the linear scale. The array must
     *            have the same size that of the color list. Values of this
     *            array must be increasing from 0 to 1. The first value must be
     *            0. The last value must be 1.
     * @param notApplicableColor
     *            the color to return when {@link #get(double)} is provided with
     *            a {@link Double#isNaN()}.
     * @param missingColor
     *            the color marking missing values.
     */
    ColorMap( final String name, final int[] colors, final double[] alphas, final int missingColor, final int notApplicableColor )
    {
        this.name = name;
        this.colors = colors;
        this.alphas = alphas;
        this.missingColor = missingColor;
        this.notApplicableColor = notApplicableColor;
        this.nColors = colors.length;
    }

    /**
     * Returns the color associated with missing values.
     *
     * @return the missing value color.
     */
    public int getMissingColor()
    {
        return missingColor;
    }

    /**
     * Returns this colormap name.
     *
     * @return the colormap name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the color associated with the specified value in the range from 0
     * to 1.
     * <p>
     * If {@code val} is {@link Double#NaN}, returns the non-applicable color.
     * If {@code val} is lower than 0, returns the first color of the colormap.
     * If {@code val} is higher than 1, returns the last color of the colormap.
     * Otherwise, linearly interpolate from the colors in the colormap.
     *
     * @param val
     *            the value.
     * @return a color.
     */
    public int get( final double val )
    {
        if ( Double.isNaN( val ) )
            return notApplicableColor;
        if ( val <= 0. || nColors == 1 )
            return colors[ 0 ];
        if ( val >= 1. )
            return colors[ nColors - 1 ];

        int i = Arrays.binarySearch( alphas, val );
        if ( i < 0 )
            i = -( i + 1 );
        else
            return colors[ i ];

        final double theta = ( val - alphas[ i - 1 ] ) / ( alphas[ i ] - alphas[ i - 1 ] );

        final int c1 = colors[ i - 1 ];
        final int r1 = (c1 >> 16) & 0xFF;
        final int g1 = (c1 >> 8) & 0xFF;
        final int b1 = (c1 >> 0) & 0xFF;
        final int c2 = colors[ i ];
        final int r2 = (c2 >> 16) & 0xFF;
        final int g2 = (c2 >> 8) & 0xFF;
        final int b2 = (c2 >> 0) & 0xFF;

        final int r = ( int ) ( ( r2 - r1 ) * theta + r1 );
        final int g = ( int ) ( ( g2 - g1 ) * theta + g1 );
        final int b = ( int ) ( ( b2 - b1 ) * theta + b1 );

        return (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF));
    }

    public int get_fast( final double val )
    {
        if ( Double.isNaN( val ) )
            return notApplicableColor;
        if ( val <= 0. || nColors == 1 )
            return colors[ 0 ];
        if ( val >= 1. )
            return colors[ nColors - 1 ];


        int i = (int)(val * (nColors - 1));
        if ( i == nColors) {
            return colors[ nColors - 1];
        }

        final double theta = ( val - alphas[ i] ) / ( alphas[ i + 1 ] - alphas[ i ] );

        final int c1 = colors[ i ];
        final int r1 = (c1 >> 16) & 0xFF;
        final int g1 = (c1 >> 8) & 0xFF;
        final int b1 = (c1 >> 0) & 0xFF;
        final int c2 = colors[ i + 1 ];
        final int r2 = (c2 >> 16) & 0xFF;
        final int g2 = (c2 >> 8) & 0xFF;
        final int b2 = (c2 >> 0) & 0xFF;

        final int r = ( int ) ( ( r2 - r1 ) * theta + r1 );
        final int g = ( int ) ( ( g2 - g1 ) * theta + g1 );
        final int b = ( int ) ( ( b2 - b1 ) * theta + b1 );

        return (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF));
    }


    public static int floats2color(float r, float g, float b) {
        return (0xFF << 24) | (((int)(r*255) & 0xFF) << 16) | (((int)(g*255) & 0xFF) << 8) | (((int)(b*255) & 0xFF));
    }

    public final static ColorMap gray()
    {
        return new ColorMap("gray", new int[]{0xFF000000, 0xFFFFFFFF}, new double[]{0.0, 1.0}, Color.RED, Color.BLUE);
    }

    /**
     * Returns the jet colormap, that interpolates colors from blue, then green,
     * then red.
     *
     * @return a new colormap.
     */
    public final static ColorMap jet()
    {
        final int[] colors = new int[] {
                floats2color( 0f, 0f, 1.f ),
                floats2color( 0f, 0.3333f, 1.f ),
                floats2color( 0f, 0.6667f, 1.f ),
                floats2color( 0f, 1.f, 1.f ),
                floats2color( 0.3333f, 1.f, 0.6667f ),
                floats2color( 0.6667f, 1.f, 0.3333f ),
                floats2color( 1.f, 1.f, 0f ),
                floats2color( 1.f, 0.6667f, 0f ),
                floats2color( 1.f, 0.3333f, 0f ),
                floats2color( 1.f, 0f, 0f ),
        };
        final double[] alphas = new double[ colors.length ];
        for ( int i = 0; i < alphas.length; i++ )
            alphas[ i ] = ( double ) i / ( colors.length - 1 );

        return new ColorMap( "jet", colors, alphas, Color.GRAY, Color.BLACK );
    }

    private static final ColorMap cubeHelix()
    {
        final float[] r = new float[] { 0.000f, 0.020f, 0.038f, 0.055f, 0.069f, 0.080f, 0.089f, 0.096f, 0.101f, 0.103f, 0.103f, 0.100f, 0.096f, 0.091f, 0.084f, 0.076f, 0.067f, 0.057f, 0.048f, 0.038f, 0.029f, 0.021f, 0.014f, 0.008f, 0.004f, 0.002f, 0.003f, 0.006f, 0.011f, 0.019f, 0.030f, 0.044f, 0.061f, 0.081f, 0.104f, 0.130f, 0.158f, 0.189f, 0.222f, 0.257f, 0.294f, 0.332f, 0.372f, 0.413f, 0.454f, 0.495f, 0.536f, 0.576f, 0.616f, 0.654f, 0.691f, 0.726f, 0.759f, 0.790f, 0.817f, 0.843f, 0.865f, 0.884f, 0.900f, 0.912f, 0.922f, 0.928f, 0.932f, 0.932f, 0.930f, 0.925f, 0.918f, 0.908f, 0.897f, 0.884f, 0.870f, 0.856f, 0.840f, 0.825f, 0.810f, 0.795f, 0.781f, 0.768f, 0.756f, 0.746f, 0.738f, 0.732f, 0.728f, 0.727f, 0.728f, 0.732f, 0.738f, 0.746f, 0.757f, 0.771f, 0.786f, 0.803f, 0.822f, 0.843f, 0.864f,
                0.887f, 0.910f, 0.933f, 0.956f, 0.978f, 1.000f };
        final float[] g = new float[] { 0.000f, 0.003f, 0.007f, 0.012f, 0.017f, 0.024f, 0.031f, 0.040f, 0.050f, 0.061f, 0.074f, 0.088f, 0.103f, 0.119f, 0.137f, 0.155f, 0.174f, 0.194f, 0.215f, 0.236f, 0.257f, 0.279f, 0.300f, 0.322f, 0.342f, 0.362f, 0.382f, 0.400f, 0.418f, 0.434f, 0.449f, 0.463f, 0.475f, 0.485f, 0.494f, 0.502f, 0.507f, 0.511f, 0.514f, 0.515f, 0.514f, 0.513f, 0.510f, 0.506f, 0.501f, 0.495f, 0.489f, 0.483f, 0.476f, 0.469f, 0.462f, 0.456f, 0.451f, 0.446f, 0.441f, 0.438f, 0.437f, 0.436f, 0.437f, 0.439f, 0.443f, 0.449f, 0.456f, 0.465f, 0.476f, 0.488f, 0.501f, 0.517f, 0.533f, 0.551f, 0.570f, 0.590f, 0.611f, 0.633f, 0.655f, 0.677f, 0.700f, 0.722f, 0.745f, 0.767f, 0.788f, 0.809f, 0.829f, 0.848f, 0.866f, 0.883f, 0.898f, 0.913f, 0.926f, 0.937f, 0.948f, 0.957f, 0.965f, 0.972f, 0.978f,
                0.983f, 0.987f, 0.991f, 0.994f, 0.997f, 1.000f };
        final float[] b = new float[] { 0.000f, 0.018f, 0.039f, 0.061f, 0.085f, 0.109f, 0.134f, 0.159f, 0.184f, 0.209f, 0.232f, 0.255f, 0.276f, 0.295f, 0.312f, 0.326f, 0.338f, 0.347f, 0.354f, 0.358f, 0.358f, 0.356f, 0.352f, 0.344f, 0.334f, 0.322f, 0.307f, 0.291f, 0.274f, 0.255f, 0.235f, 0.215f, 0.195f, 0.175f, 0.156f, 0.138f, 0.121f, 0.107f, 0.094f, 0.083f, 0.076f, 0.071f, 0.069f, 0.071f, 0.076f, 0.084f, 0.096f, 0.112f, 0.131f, 0.154f, 0.180f, 0.209f, 0.240f, 0.275f, 0.312f, 0.351f, 0.391f, 0.433f, 0.476f, 0.519f, 0.563f, 0.606f, 0.649f, 0.691f, 0.732f, 0.771f, 0.808f, 0.842f, 0.875f, 0.904f, 0.931f, 0.955f, 0.975f, 0.993f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 0.997f, 0.987f, 0.978f, 0.969f, 0.961f, 0.954f, 0.949f, 0.945f, 0.943f, 0.943f,
                0.946f, 0.952f, 0.960f, 0.970f, 0.984f, 1.000f };
        final int[] colors = new int[ r.length ];
        final double[] alphas = new double[ r.length ];
        for ( int i = 0; i < colors.length; i++ )
        {
            colors[ i ] = floats2color( r[ i ], g[ i ], b[ i ] );
            alphas[ i ] = i * 0.01;
        }

        return new ColorMap( "cube-helix", colors, alphas, Color.YELLOW, Color.GRAY );
    }

    private static final ColorMap parula()
    {
        // r0, g0, b0, r1, g1, b1, ...
        final float[] values = new float[] { 0.2081f, 0.1663f, 0.5292f, 0.2091f, 0.1721f, 0.5411f, 0.2101f, 0.1779f, 0.5530f, 0.2109f, 0.1837f, 0.5650f, 0.2116f, 0.1895f, 0.5771f, 0.2121f, 0.1954f, 0.5892f, 0.2124f, 0.2013f, 0.6013f, 0.2125f, 0.2072f, 0.6135f, 0.2123f, 0.2132f, 0.6258f, 0.2118f, 0.2192f, 0.6381f, 0.2111f, 0.2253f, 0.6505f, 0.2099f, 0.2315f, 0.6629f, 0.2084f, 0.2377f, 0.6753f, 0.2063f, 0.2440f, 0.6878f, 0.2038f, 0.2503f, 0.7003f, 0.2006f, 0.2568f, 0.7129f, 0.1968f, 0.2632f, 0.7255f, 0.1921f, 0.2698f, 0.7381f, 0.1867f, 0.2764f, 0.7507f, 0.1802f, 0.2832f, 0.7634f, 0.1728f, 0.2902f, 0.7762f, 0.1641f, 0.2975f, 0.7890f, 0.1541f, 0.3052f, 0.8017f, 0.1427f, 0.3132f, 0.8145f, 0.1295f, 0.3217f, 0.8269f, 0.1147f, 0.3306f, 0.8387f, 0.0986f, 0.3397f, 0.8495f, 0.0816f, 0.3486f, 0.8588f,
                0.0646f, 0.3572f, 0.8664f, 0.0482f, 0.3651f, 0.8722f, 0.0329f, 0.3724f, 0.8765f, 0.0213f, 0.3792f, 0.8796f, 0.0136f, 0.3853f, 0.8815f, 0.0086f, 0.3911f, 0.8827f, 0.0060f, 0.3965f, 0.8833f, 0.0051f, 0.4017f, 0.8834f, 0.0054f, 0.4066f, 0.8831f, 0.0067f, 0.4113f, 0.8825f, 0.0089f, 0.4159f, 0.8816f, 0.0116f, 0.4203f, 0.8805f, 0.0148f, 0.4246f, 0.8793f, 0.0184f, 0.4288f, 0.8779f, 0.0223f, 0.4329f, 0.8763f, 0.0264f, 0.4370f, 0.8747f, 0.0306f, 0.4410f, 0.8729f, 0.0349f, 0.4449f, 0.8711f, 0.0394f, 0.4488f, 0.8692f, 0.0437f, 0.4526f, 0.8672f, 0.0477f, 0.4564f, 0.8652f, 0.0514f, 0.4602f, 0.8632f, 0.0549f, 0.4640f, 0.8611f, 0.0582f, 0.4677f, 0.8589f, 0.0612f, 0.4714f, 0.8568f, 0.0640f, 0.4751f, 0.8546f, 0.0666f, 0.4788f, 0.8525f, 0.0689f, 0.4825f, 0.8503f, 0.0710f, 0.4862f, 0.8481f,
                0.0729f, 0.4899f, 0.8460f, 0.0746f, 0.4937f, 0.8439f, 0.0761f, 0.4974f, 0.8418f, 0.0773f, 0.5012f, 0.8398f, 0.0782f, 0.5051f, 0.8378f, 0.0789f, 0.5089f, 0.8359f, 0.0794f, 0.5129f, 0.8341f, 0.0795f, 0.5169f, 0.8324f, 0.0793f, 0.5210f, 0.8308f, 0.0788f, 0.5251f, 0.8293f, 0.0778f, 0.5295f, 0.8280f, 0.0764f, 0.5339f, 0.8270f, 0.0746f, 0.5384f, 0.8261f, 0.0724f, 0.5431f, 0.8253f, 0.0698f, 0.5479f, 0.8247f, 0.0668f, 0.5527f, 0.8243f, 0.0636f, 0.5577f, 0.8239f, 0.0600f, 0.5627f, 0.8237f, 0.0562f, 0.5677f, 0.8234f, 0.0523f, 0.5727f, 0.8231f, 0.0484f, 0.5777f, 0.8228f, 0.0445f, 0.5826f, 0.8223f, 0.0408f, 0.5874f, 0.8217f, 0.0372f, 0.5922f, 0.8209f, 0.0342f, 0.5968f, 0.8198f, 0.0317f, 0.6012f, 0.8186f, 0.0296f, 0.6055f, 0.8171f, 0.0279f, 0.6097f, 0.8154f, 0.0265f, 0.6137f, 0.8135f,
                0.0255f, 0.6176f, 0.8114f, 0.0248f, 0.6214f, 0.8091f, 0.0243f, 0.6250f, 0.8066f, 0.0239f, 0.6285f, 0.8039f, 0.0237f, 0.6319f, 0.8010f, 0.0235f, 0.6352f, 0.7980f, 0.0233f, 0.6384f, 0.7948f, 0.0231f, 0.6415f, 0.7916f, 0.0230f, 0.6445f, 0.7881f, 0.0229f, 0.6474f, 0.7846f, 0.0227f, 0.6503f, 0.7810f, 0.0227f, 0.6531f, 0.7773f, 0.0232f, 0.6558f, 0.7735f, 0.0238f, 0.6585f, 0.7696f, 0.0246f, 0.6611f, 0.7656f, 0.0263f, 0.6637f, 0.7615f, 0.0282f, 0.6663f, 0.7574f, 0.0306f, 0.6688f, 0.7532f, 0.0338f, 0.6712f, 0.7490f, 0.0373f, 0.6737f, 0.7446f, 0.0418f, 0.6761f, 0.7402f, 0.0467f, 0.6784f, 0.7358f, 0.0516f, 0.6808f, 0.7313f, 0.0574f, 0.6831f, 0.7267f, 0.0629f, 0.6854f, 0.7221f, 0.0692f, 0.6877f, 0.7173f, 0.0755f, 0.6899f, 0.7126f, 0.0820f, 0.6921f, 0.7078f, 0.0889f, 0.6943f, 0.7029f,
                0.0956f, 0.6965f, 0.6979f, 0.1031f, 0.6986f, 0.6929f, 0.1104f, 0.7007f, 0.6878f, 0.1180f, 0.7028f, 0.6827f, 0.1258f, 0.7049f, 0.6775f, 0.1335f, 0.7069f, 0.6723f, 0.1418f, 0.7089f, 0.6669f, 0.1499f, 0.7109f, 0.6616f, 0.1585f, 0.7129f, 0.6561f, 0.1671f, 0.7148f, 0.6507f, 0.1758f, 0.7168f, 0.6451f, 0.1849f, 0.7186f, 0.6395f, 0.1938f, 0.7205f, 0.6338f, 0.2033f, 0.7223f, 0.6281f, 0.2128f, 0.7241f, 0.6223f, 0.2224f, 0.7259f, 0.6165f, 0.2324f, 0.7275f, 0.6107f, 0.2423f, 0.7292f, 0.6048f, 0.2527f, 0.7308f, 0.5988f, 0.2631f, 0.7324f, 0.5929f, 0.2735f, 0.7339f, 0.5869f, 0.2845f, 0.7354f, 0.5809f, 0.2953f, 0.7368f, 0.5749f, 0.3064f, 0.7381f, 0.5689f, 0.3177f, 0.7394f, 0.5630f, 0.3289f, 0.7406f, 0.5570f, 0.3405f, 0.7417f, 0.5512f, 0.3520f, 0.7428f, 0.5453f, 0.3635f, 0.7438f, 0.5396f,
                0.3753f, 0.7446f, 0.5339f, 0.3869f, 0.7454f, 0.5283f, 0.3986f, 0.7461f, 0.5229f, 0.4103f, 0.7467f, 0.5175f, 0.4218f, 0.7473f, 0.5123f, 0.4334f, 0.7477f, 0.5072f, 0.4447f, 0.7482f, 0.5021f, 0.4561f, 0.7485f, 0.4972f, 0.4672f, 0.7487f, 0.4924f, 0.4783f, 0.7489f, 0.4877f, 0.4892f, 0.7491f, 0.4831f, 0.5000f, 0.7491f, 0.4786f, 0.5106f, 0.7492f, 0.4741f, 0.5212f, 0.7492f, 0.4698f, 0.5315f, 0.7491f, 0.4655f, 0.5418f, 0.7490f, 0.4613f, 0.5519f, 0.7489f, 0.4571f, 0.5619f, 0.7487f, 0.4531f, 0.5718f, 0.7485f, 0.4490f, 0.5816f, 0.7482f, 0.4451f, 0.5913f, 0.7479f, 0.4412f, 0.6009f, 0.7476f, 0.4374f, 0.6103f, 0.7473f, 0.4335f, 0.6197f, 0.7469f, 0.4298f, 0.6290f, 0.7465f, 0.4261f, 0.6382f, 0.7460f, 0.4224f, 0.6473f, 0.7456f, 0.4188f, 0.6564f, 0.7451f, 0.4152f, 0.6653f, 0.7446f, 0.4116f,
                0.6742f, 0.7441f, 0.4081f, 0.6830f, 0.7435f, 0.4046f, 0.6918f, 0.7430f, 0.4011f, 0.7004f, 0.7424f, 0.3976f, 0.7091f, 0.7418f, 0.3942f, 0.7176f, 0.7412f, 0.3908f, 0.7261f, 0.7405f, 0.3874f, 0.7346f, 0.7399f, 0.3840f, 0.7430f, 0.7392f, 0.3806f, 0.7513f, 0.7385f, 0.3773f, 0.7596f, 0.7378f, 0.3739f, 0.7679f, 0.7372f, 0.3706f, 0.7761f, 0.7364f, 0.3673f, 0.7843f, 0.7357f, 0.3639f, 0.7924f, 0.7350f, 0.3606f, 0.8005f, 0.7343f, 0.3573f, 0.8085f, 0.7336f, 0.3539f, 0.8166f, 0.7329f, 0.3506f, 0.8246f, 0.7322f, 0.3472f, 0.8325f, 0.7315f, 0.3438f, 0.8405f, 0.7308f, 0.3404f, 0.8484f, 0.7301f, 0.3370f, 0.8563f, 0.7294f, 0.3336f, 0.8642f, 0.7288f, 0.3300f, 0.8720f, 0.7282f, 0.3265f, 0.8798f, 0.7276f, 0.3229f, 0.8877f, 0.7271f, 0.3193f, 0.8954f, 0.7266f, 0.3156f, 0.9032f, 0.7262f, 0.3117f,
                0.9110f, 0.7259f, 0.3078f, 0.9187f, 0.7256f, 0.3038f, 0.9264f, 0.7256f, 0.2996f, 0.9341f, 0.7256f, 0.2953f, 0.9417f, 0.7259f, 0.2907f, 0.9493f, 0.7264f, 0.2859f, 0.9567f, 0.7273f, 0.2808f, 0.9639f, 0.7285f, 0.2754f, 0.9708f, 0.7303f, 0.2696f, 0.9773f, 0.7326f, 0.2634f, 0.9831f, 0.7355f, 0.2570f, 0.9882f, 0.7390f, 0.2504f, 0.9922f, 0.7431f, 0.2437f, 0.9952f, 0.7476f, 0.2373f, 0.9973f, 0.7524f, 0.2310f, 0.9986f, 0.7573f, 0.2251f, 0.9991f, 0.7624f, 0.2195f, 0.9990f, 0.7675f, 0.2141f, 0.9985f, 0.7726f, 0.2090f, 0.9976f, 0.7778f, 0.2042f, 0.9964f, 0.7829f, 0.1995f, 0.9950f, 0.7880f, 0.1949f, 0.9933f, 0.7931f, 0.1905f, 0.9914f, 0.7981f, 0.1863f, 0.9894f, 0.8032f, 0.1821f, 0.9873f, 0.8083f, 0.1780f, 0.9851f, 0.8133f, 0.1740f, 0.9828f, 0.8184f, 0.1700f, 0.9805f, 0.8235f, 0.1661f,
                0.9782f, 0.8286f, 0.1622f, 0.9759f, 0.8337f, 0.1583f, 0.9736f, 0.8389f, 0.1544f, 0.9713f, 0.8441f, 0.1505f, 0.9692f, 0.8494f, 0.1465f, 0.9672f, 0.8548f, 0.1425f, 0.9654f, 0.8603f, 0.1385f, 0.9638f, 0.8659f, 0.1343f, 0.9623f, 0.8716f, 0.1301f, 0.9611f, 0.8774f, 0.1258f, 0.9600f, 0.8834f, 0.1215f, 0.9593f, 0.8895f, 0.1171f, 0.9588f, 0.8958f, 0.1126f, 0.9586f, 0.9022f, 0.1082f, 0.9587f, 0.9088f, 0.1036f, 0.9591f, 0.9155f, 0.0990f, 0.9599f, 0.9225f, 0.0944f, 0.9610f, 0.9296f, 0.0897f, 0.9624f, 0.9368f, 0.0850f, 0.9641f, 0.9443f, 0.0802f, 0.9662f, 0.9518f, 0.0753f, 0.9685f, 0.9595f, 0.0703f, 0.9710f, 0.9673f, 0.0651f, 0.9736f, 0.9752f, 0.0597f, 0.9763f, 0.9831f, 0.0538f };
        final int ncolors = values.length / 3;
        final int[] colors = new int[ ncolors ];
        final double[] alphas = new double[ ncolors ];
        int j = 0;
        for ( int i = 0; i < ncolors; i++ )
        {
            colors[ i ] = floats2color( values[ j++ ], values[ j++ ], values[ j++ ] );
            alphas[ i ] = ( double ) i / ( ncolors - 1 );
        }

        return new ColorMap( "parula", colors, alphas, Color.BLACK, Color.GRAY );
    }

    private static final ColorMap seismic()
    {
        // From MatPlotLib
        final float[][] vals = new float[][] { { 0.0f, 0.0f, 0.3f }, { 0.0f, 0.0f, 1.0f }, { 1.0f, 1.0f, 1.0f }, { 1.0f, 0.0f, 0.0f }, { 0.5f, 0.0f, 0.0f } };
        final int ncolors = vals.length;
        final int[] colors = new int[ ncolors ];
        final double[] alphas = new double[ ncolors ];
        for ( int i = 0; i < ncolors; i++ )
        {
            colors[ i ] = floats2color( vals[ i ][ 0 ], vals[ i ][ 1 ], vals[ i ][ 2 ] );
            alphas[ i ] = ( double ) i / ( ncolors - 1 );
        }

        return new ColorMap( "seismic", colors, alphas, Color.BLACK, Color.GRAY );
    }

    private static final ColorMap viridis()
    {
        final float[][] vals = MatplotlibColormaps._viridis_data;
        final int ncolors = vals.length;
        final int[] colors = new int[ ncolors ];
        final double[] alphas = new double[ ncolors ];
        for ( int i = 0; i < ncolors; i++ )
        {
            colors[ i ] = floats2color( vals[ i ][ 0 ], vals[ i ][ 1 ], vals[ i ][ 2 ] );
            alphas[ i ] = ( double ) i / ( ncolors - 1 );
        }
        return new ColorMap( "viridis", colors, alphas, Color.BLACK, Color.GRAY );
    }

    private static final ColorMap magma()
    {
        final float[][] vals = MatplotlibColormaps._magma_data;
        final int ncolors = vals.length;
        final int[] colors = new int[ ncolors ];
        final double[] alphas = new double[ ncolors ];
        for ( int i = 0; i < ncolors; i++ )
        {
            colors[ i ] = floats2color( vals[ i ][ 0 ], vals[ i ][ 1 ], vals[ i ][ 2 ] );
            alphas[ i ] = ( double ) i / ( ncolors - 1 );
        }
        return new ColorMap( "magma", colors, alphas, Color.BLACK, Color.GRAY );
    }

    private static final ColorMap inferno()
    {
        final float[][] vals = MatplotlibColormaps._inferno_data;
        final int ncolors = vals.length;
        final int[] colors = new int[ ncolors ];
        final double[] alphas = new double[ ncolors ];
        for ( int i = 0; i < ncolors; i++ )
        {
            colors[ i ] = floats2color( vals[ i ][ 0 ], vals[ i ][ 1 ], vals[ i ][ 2 ] );
            alphas[ i ] = ( double ) i / ( ncolors - 1 );
        }
        return new ColorMap( "inferno", colors, alphas, Color.BLACK, Color.GRAY );
    }

    private static final ColorMap plasma()
    {
        final float[][] vals = MatplotlibColormaps._plasma_data;
        final int ncolors = vals.length;
        final int[] colors = new int[ ncolors ];
        final double[] alphas = new double[ ncolors ];
        for ( int i = 0; i < ncolors; i++ )
        {
            colors[ i ] = floats2color( vals[ i ][ 0 ], vals[ i ][ 1 ], vals[ i ][ 2 ] );
            alphas[ i ] = ( double ) i / ( ncolors - 1 );
        }
        return new ColorMap( "plasma", colors, alphas, Color.BLACK, Color.GRAY );
    }

    private static final ColorMap twilight()
    {
        final float[][] vals = MatplotlibColormaps._twilight_data;
        final int ncolors = vals.length;
        final int[] colors = new int[ ncolors ];
        final double[] alphas = new double[ ncolors ];
        for ( int i = 0; i < ncolors; i++ )
        {
            colors[ i ] = floats2color( vals[ i ][ 0 ], vals[ i ][ 1 ], vals[ i ][ 2 ] );
            alphas[ i ] = ( double ) i / ( ncolors - 1 );
        }
        return new ColorMap( "twilight", colors, alphas, Color.BLACK, Color.GRAY );
    }

    private static final ColorMap turbo()
    {
        final float[][] vals = MatplotlibColormaps._turbo_data;
        final int ncolors = vals.length;
        final int[] colors = new int[ ncolors ];
        final double[] alphas = new double[ ncolors ];
        for ( int i = 0; i < ncolors; i++ )
        {
            colors[ i ] = floats2color( vals[ i ][ 0 ], vals[ i ][ 1 ], vals[ i ][ 2 ] );
            alphas[ i ] = ( double ) i / ( ncolors - 1 );
        }
        return new ColorMap( "turbo", colors, alphas, Color.BLACK, Color.GRAY );
    }

    private static final ColorMap rainbow()
    {
        final float[][] vals = MatplotlibColormaps._rainbow_data;
        final int ncolors = vals.length;
        final int[] colors = new int[ ncolors ];
        final double[] alphas = new double[ ncolors ];
        for ( int i = 0; i < ncolors; i++ )
        {
            colors[ i ] = floats2color( vals[ i ][ 0 ], vals[ i ][ 1 ], vals[ i ][ 2 ] );
            alphas[ i ] = ( double ) i / ( ncolors - 1 );
        }
        return new ColorMap( "rainbow", colors, alphas, Color.BLACK, Color.GRAY );
    }

    private static final ColorMap better_rainbow()
    {
        final float[][] vals = OtherColormaps._better_rainbow_data;
        final int ncolors = vals.length;
        final int[] colors = new int[ ncolors ];
        final double[] alphas = new double[ ncolors ];
        for ( int i = 0; i < ncolors; i++ )
        {
            colors[ i ] = floats2color( vals[ i ][ 0 ], vals[ i ][ 1 ], vals[ i ][ 2 ] );
            alphas[ i ] = ( double ) i / ( ncolors - 1 );
        }
        return new ColorMap( "better rainbow", colors, alphas, Color.BLACK, Color.GRAY );
    }
}
