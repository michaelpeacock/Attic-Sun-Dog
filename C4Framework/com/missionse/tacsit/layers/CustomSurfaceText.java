/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package com.missionse.tacsit.layers;

import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.AbstractSurfaceObject;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GeographicText;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.TextRenderer;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Renders a string of text on the surface of the globe. The text will appear draped over terrain. Surface text is drawn
 * at a constant geographic size: it will appear larger when the view zooms in on the text and smaller when the view
 * zooms out.
 *
 * @author pabercrombie & kshea
 * @version $Id: CustomSurfaceText.java 1418 2013-06-07 19:26:02Z tgaskins $
 */
// TODO: add support for heading
public class CustomSurfaceText extends AbstractSurfaceObject implements GeographicText, Movable
{
    /** Default text size. */
    public final static double DEFAULT_TEXT_SIZE_IN_METERS = 1000;
    /** Default font. */
    public static final Font DEFAULT_FONT = Font.decode("Arial-BOLD-24");
    /** Default text color. */
    public static final Color DEFAULT_COLOR = Color.WHITE;
    /** Default offset. The default offset centers the text on its geographic position both horizontally and vertically. */
    public static final Offset DEFAULT_OFFSET = new Offset(-0.5d, -0.5d, AVKey.FRACTION, AVKey.FRACTION);

    /** The text to draw. */
    protected CharSequence text;
    /** Location at which to draw the text. */
    protected Position location;
    /** The height of the text in meters. */
    protected double textSizeInMeters = DEFAULT_TEXT_SIZE_IN_METERS;

    /** Font to use to draw the text. Defaults to {@link #DEFAULT_FONT}. */
    protected Font font = DEFAULT_FONT;
    /** Color to use to draw the text. Defaults to {@link #DEFAULT_COLOR}. */
    protected Color color = DEFAULT_COLOR;
    /** Background color for the text. By default color will be generated to contrast with the text color. */
    protected Color bgColor;
    /** Text priority. Can be used to implement text culling. */
    protected double priority;
    /** Offset that specifies where to place the text in relation to it's geographic position. */
    protected Offset offset = DEFAULT_OFFSET;

    // Computed each time text is rendered
    /** Bounds of the text in pixels. */
    protected Rectangle2D textBounds;
    /** Geographic size of a pixel. */
    protected double pixelSizeInMeters;
    /** Scaling factor applied to the text to maintain a constant geographic size. */
    protected double scale;

    /**
     * Create a new surface text object.
     *
     * @param text     Text to draw.
     * @param position Geographic location at which to draw the text.
     */
    public CustomSurfaceText(String text, Position position)
    {
        this.setText(text);
        this.setPosition(position);
    }

    /**
     * Create a new surface text object.
     *
     * @param text     Text to draw.
     * @param position Geographic location at which to draw the text.
     * @param font     Font to use when drawing text.
     * @param color    Color to use when drawing text.
     */
    public CustomSurfaceText(String text, Position position, Font font, Color color)
    {
        this.setText(text);
        this.setPosition(position);
        this.setFont(font);
        this.setColor(color);
    }

    /** {@inheritDoc} */
    public CharSequence getText()
    {
        return this.text;
    }

    /** {@inheritDoc} */
    public void setText(CharSequence text)
    {
        if (text == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.text = text;
        this.textBounds = null; // Need to recompute bounds
        this.updateModifiedTime();
    }

    /** {@inheritDoc} */
    public Position getPosition()
    {
        return this.location;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The offset determines how the text is placed relative to this position. The default offset centers the text on
     * the position both horizontally and vertically.
     *
     * @see #setOffset(Offset)
     */
    public void setPosition(Position position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.location = position;
        this.updateModifiedTime();
    }

    /** {@inheritDoc} */
    public Font getFont()
    {
        return this.font;
    }

    /** {@inheritDoc} */
    public void setFont(Font font)
    {
        if (font == null)
        {
            String message = Logging.getMessage("nullValue.FontIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Only set the font if it is different than the active font
        if (!font.equals(this.font))
        {
            this.font = font;
            this.textBounds = null; // Need to recompute bounds
            this.updateModifiedTime();
        }
    }

    /** {@inheritDoc} */
    public Color getColor()
    {
        return this.color;
    }

    /** {@inheritDoc} */
    public void setColor(Color color)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!color.equals(this.color))
        {
            this.color = color;
            this.updateModifiedTime();
        }
    }

    /** {@inheritDoc} */
    public Color getBackgroundColor()
    {
        return this.bgColor;
    }

    /** {@inheritDoc} */
    public void setBackgroundColor(Color background)
    {
        if (background == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (bgColor == null || !bgColor.equals(background))
        {
            this.bgColor = background;
            this.updateModifiedTime();
        }
    }

    /** {@inheritDoc} */
    public void setPriority(double priority)
    {
        this.priority = priority;
    }

    /** {@inheritDoc} */
    public double getPriority()
    {
        return this.priority;
    }

    /**
     * Returns the text offset. The offset determines how to position the text relative to its geographic position.
     *
     * @return the text offset.
     *
     * @see #setOffset(Offset)
     */
    public Offset getOffset()
    {
        return this.offset;
    }

    /**
     * Specifies a location relative to the label position at which to align the label. The label text begins at the
     * point indicated by the offset. An offset of (0, 0) aligns the left baseline of the text with the position. An
     * offset of (-0.5, -0.5) fraction aligns the center of the text with the position.
     * <p/>
     * A pixel based offset is interpreted based on the geographic size of the text. For example, if the text rendered
     * "normally" in two dimensions would be 20 pixels tall, and the geographic text is 100 meters tall, then each pixel
     * of text corresponds to 5 meters. So an offset of 2 pixels would correspond to a geographic offset of 10 meters.
     *
     * @param offset Offset that controls where to position the label relative to its geographic location.
     */
    public void setOffset(Offset offset)
    {
        if (offset == null)
        {
            String message = Logging.getMessage("nullValue.OffsetIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!offset.equals(this.offset))
        {
            this.offset = offset;
            this.updateModifiedTime();
        }
    }

    public double getTextSize()
    {
        return this.textSizeInMeters;
    }

    public void setTextSize(double meters)
    {
        this.textSizeInMeters = meters;
    }

    /** {@inheritDoc} */
    @Override
    public void preRender(DrawContext dc)
    {
        if (this.textBounds == null)
        {
            this.updateTextBounds(dc);
        }

        super.preRender(dc);
    }

    /** {@inheritDoc} */
    public Position getReferencePosition()
    {
        return new Position(this.location, 0);
    }

    /** {@inheritDoc} */
    public void move(Position position)
    {
        Position refPos = this.getReferencePosition();
        if (refPos == null)
            return;

        this.moveTo(refPos.add(position));
    }

    /** {@inheritDoc} */
    public void moveTo(Position position)
    {
        this.setPosition(position);
    }

    /** {@inheritDoc} */
    public java.util.List<Sector> getSectors(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return Arrays.asList(this.computeSector(dc));
    }

    /** {@inheritDoc} */
    protected void drawGeographic(DrawContext dc, SurfaceTileDrawContext sdc)
    {
        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushAttrib(gl,
            GL2.GL_CURRENT_BIT       // For current color (used by JOGL TextRenderer).
                | GL2.GL_TRANSFORM_BIT); // For matrix mode.
        ogsh.pushModelview(gl);
        try
        {
            this.computeGeometry(dc, sdc);

            if (this.isSmall())
                return;

            this.applyDrawTransform(dc, sdc);
            this.drawText(dc);
        }
        finally
        {
            ogsh.pop(gl);
        }
    }

    /**
     * Draw the text.
     *
     * @param dc Current draw context.
     */
    protected void drawText(DrawContext dc)
    {
        TextRenderer tr = this.getTextRenderer(dc);

        Point2D point = this.getOffset().computeOffset(this.textBounds.getWidth(), this.textBounds.getHeight(), null,
            null);

        int x = (int) point.getX();
        int y = (int) point.getY();

        try
        {
            tr.begin3DRendering();

            Color bgColor = this.determineBackgroundColor(this.color);
            CharSequence text = this.getText();

            tr.setColor(bgColor);
            tr.draw(text, x + 1, y - 1);
            tr.setColor(this.getColor());
            tr.draw(text, x, y);
        }
        finally
        {
            tr.end3DRendering();
        }
    }

    /**
     * Compute the text size and position.
     *
     * @param dc  Current draw context.
     * @param sdc Current surface tile draw context.
     */
    protected void computeGeometry(DrawContext dc, SurfaceTileDrawContext sdc)
    {
        // Determine the geographic size of a pixel in the tile
        this.pixelSizeInMeters = this.computePixelSize(dc, sdc);

        // Determine how big the text would be without scaling
        double fullHeightInMeters = this.pixelSizeInMeters * this.textBounds.getHeight();

        // Calculate a scale to make the text the size we want (a constant geographic size)
        this.scale = this.textSizeInMeters / fullHeightInMeters;
    }

    /**
     * Apply a transform to the GL state to draw the text at the proper location and scale.
     *
     * @param dc  Current draw context.
     * @param sdc Current surface tile draw context.
     */
    protected void applyDrawTransform(DrawContext dc, SurfaceTileDrawContext sdc)
    {
        Vec4 point = new Vec4(this.location.getLongitude().degrees, this.location.getLatitude().degrees, 1);
        point = point.transformBy4(sdc.getModelviewMatrix());

        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

        // Translate to location point
        gl.glTranslated(point.x(), point.y(), point.z());

        // Apply the scaling factor to draw the text at the correct geographic size
        gl.glScaled(this.scale, this.scale, 1d);
    }

    /**
     * Determine if the text is too small to draw.
     *
     * @return {@code true} if the height of the text is less than one pixel.
     */
    protected boolean isSmall()
    {
        return this.scale * this.textSizeInMeters < this.pixelSizeInMeters;
    }

    /**
     * Compute the size of a pixel in the surface tile.
     *
     * @param dc  Current draw context.
     * @param sdc Current surface tile draw context.
     *
     * @return The size of a tile pixel in meters.
     */
    protected double computePixelSize(DrawContext dc, SurfaceTileDrawContext sdc)
    {
        return dc.getGlobe().getRadius() * sdc.getSector().getDeltaLatRadians() / sdc.getViewport().height;
    }

    /**
     * Determine the text background color. This method returns the user specified background color, or a computed
     * default color if the user has not set a background color.
     *
     * @param color text color.
     *
     * @return the user specified background color, or a default color that contrasts with the text color.
     */
    protected Color determineBackgroundColor(Color color)
    {
        // If the app specified a background color, use that.
        Color bgColor = this.getBackgroundColor();
        if (bgColor != null)
            return bgColor;

        // Otherwise compute a color that contrasts with the text color.
        return this.computeBackgroundColor(color);
    }

    /**
     * Compute a background color that contrasts with the text color.
     *
     * @param color text color.
     *
     * @return a color that contrasts with the text color.
     */
    protected Color computeBackgroundColor(Color color)
    {
        // Otherwise compute a color that contrasts with the text color.
        float[] colorArray = new float[4];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), colorArray);

        if (colorArray[2] > 0.5)
            return new Color(0, 0, 0, 0.7f);
        else
            return new Color(1, 1, 1, 0.7f);
    }

    /**
     * Compute the sector covered by this surface text.
     *
     * @param dc Current draw context.
     *
     * @return The sector covered by the surface text.
     */
    protected Sector[] computeSector(DrawContext dc)
    {
        // Compute text extent depending on distance from eye
        Globe globe = dc.getGlobe();

        double widthInPixels = this.textBounds.getWidth();
        double heightInPixels = this.textBounds.getHeight();

        double heightInMeters = this.textSizeInMeters;
        double widthInMeters = heightInMeters * (widthInPixels / heightInPixels);

        double radius = globe.getRadius();
        double heightInRadians = heightInMeters / radius;
        double widthInRadians = widthInMeters / radius;

        // Compute the offset from the reference position. Convert pixels to meters based on the geographic size
        // of the text.
        Point2D point = this.getOffset().computeOffset(widthInPixels, heightInPixels, null, null);

        double metersPerPixel = heightInMeters / heightInPixels;

        double dxRadians = (point.getX() * metersPerPixel) / radius;
        double dyRadians = (point.getY() * metersPerPixel) / radius;

        Angle minLat = this.location.latitude.addRadians(dyRadians);
        Angle maxLat = this.location.latitude.addRadians(heightInRadians + dyRadians);
        Angle minLong = this.location.longitude.addRadians(dxRadians);
        Angle maxLong = this.location.longitude.addRadians(widthInRadians + dxRadians);
        
        minLat = minLat.normalizedLatitude();
        maxLat = maxLat.normalizedLatitude();
        minLong = minLong.normalizedLongitude();
        maxLong = maxLong.normalizedLongitude();
        
        LatLon minLatLon = new LatLon(minLat,minLong);
        LatLon maxLatLon = new LatLon(maxLat,maxLong);
        boolean crossesDateline = LatLon.locationsCrossDateline(minLatLon, maxLatLon);
        if (crossesDateline) {
        	List<LatLon> list = new ArrayList<>();
            list.add(minLatLon);
            list.add(maxLatLon);
            return Sector.splitBoundingSectors(list);
        }
        
        return new Sector[]{new Sector(minLat,maxLat,minLong,maxLong)};
        
        /*return new Sector(
            this.location.latitude.addRadians(dyRadians),
            this.location.latitude.addRadians(heightInRadians + dyRadians),
            this.location.longitude.addRadians(dxRadians),
            this.location.longitude.addRadians(widthInRadians + dxRadians)
        );*/
    }

    /**
     * Get the text renderer to use to draw text.
     *
     * @param dc Current draw context.
     *
     * @return The text renderer that will be used to draw the surface text.
     */
    protected TextRenderer getTextRenderer(DrawContext dc)
    {
        return OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(), this.getFont(), true, false, false);
    }

    /**
     * Determine the text bounds.
     *
     * @param dc Current draw context.
     */
    protected void updateTextBounds(DrawContext dc)
    {
        this.textBounds = this.getTextRenderer(dc).getBounds(this.text);
    }

}

