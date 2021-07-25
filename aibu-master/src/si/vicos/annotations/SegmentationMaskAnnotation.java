package si.vicos.annotations;

import java.awt.*;
import java.awt.geom.Point2D;
import java.text.ParseException;
import java.util.*;
import java.util.List;

import org.coffeeshop.string.StringUtils;
import si.vicos.annotations.editor.AnnotatedImageFigure;

/**
 * The Class SegmentationMaskAnnotation.
 */
public class SegmentationMaskAnnotation extends ShapeAnnotation {

    /** The mask. */
    private boolean[][] mask;

    /** The offset*/
    private int x0;
    private int y0;
    private int width;
    private int height;

    /**
     * Instantiates a new segmenation mask annotation.
     */
    public SegmentationMaskAnnotation() {
        mask = new boolean[1000][1000];
        x0 = 0;
        y0 = 0;
        width = 1000;
        height = 1000;
    }

    public SegmentationMaskAnnotation(boolean[][] mask) {
        if(mask != null)
            this.mask = mask;
        else
            mask = new boolean[1000][1000];
    }

    public SegmentationMaskAnnotation(Vector<Point> points) {
        mask = new boolean[1000][1000];
        x0 = 0;
        y0 = 0;
        width = 1000;
        height = 1000;
        for (Point point:points) {
            this.mask[point.y][point.x] = !this.mask[point.y][point.x];
        }
    }

    /**
     * Constructor for a mask from rle
     * */
    public SegmentationMaskAnnotation(String[] tokens) {
        this.x0 = Integer.parseInt(String.valueOf(tokens[0].charAt(1)));
        this.y0 = Integer.parseInt(tokens[1]);
        this.width = Integer.parseInt(tokens[2]);
        this.height = Integer.parseInt(tokens[3]);
        this.mask = rle_to_mask(tokens,width,height);
    }

    private static boolean[][] rle_to_mask(String[] rle, int width, int height) {
        /**
         *     rle: input rle mask encoding
         *     each evenly-indexed element represents number of consecutive 0s
         *     each oddly indexed element represents number of consecutive 1s
         *     width and height are dimensions of the mask
         *     output: 2-D binary mask
         *     */

        boolean[][] mask = new boolean[height][width];

        // set id of the last different element to the beginning of the vector
        int idx_ = 0;
        for (int i = 4; i < rle.length; i++) {
            if(i % 2 != 0)
                //write as many trues as RLE says (falses are already in the vector)
                for (int j = 0; j < Integer.parseInt(rle[i]); j++) {
                    mask[(idx_ + j) / width][(idx_ + j) % width] = true;
                }
            idx_ += Integer.parseInt(rle[i]);
        }
        return mask;
    }

    /** Getter for the mask */
    public boolean[][] getMask() {
        return mask;
    }

    /** Adds new points to the mask */
    public void addPoints(Vector<Point> points) {
        if (this.mask == null)
            mask = new boolean[1000][1000];
        for (Point point:points)
            this.mask[point.y][point.x] = !this.mask[point.y][point.x];
    }

    /*
     * (non-Javadoc)
     *
     * @see si.vicos.annotations.Annotation#pack()
     */
    @Override
    public String pack() {
        return isNull() ? "" : String.format(SERIALIZATION_LOCALE,
                "%s", rle_to_string(mask_to_rle()));
    }

    private String rle_to_string(int[] mask_to_rle) {
        StringBuilder sb = new StringBuilder();
        sb.append("m");
        sb.append(String.format("%d,%d,%d,%d,", x0, y0, width, height));
        for (int i = 0; i < mask_to_rle.length; i++) {
            sb.append(String.format("%d,",mask_to_rle[i]));
        }
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * The mask to rle function
     */
    private int[] mask_to_rle(){
        /**
         * Input: 2-D array
         * Output: array of numbers (1st number = #0s, 2nd number = #1s, 3rd number = #0s, ...)
         */
        // reshape mask to a list
        List<Boolean> l = new ArrayList<>();
        for (boolean[] row : this.mask) {
            for (boolean pixel : row) {
                l.add(pixel);
            }
        }
        // output is empty at the begining
        List<Integer> rle = new ArrayList<>();

        // index of the last different element
        int last_idx = 0;

        // check if first element is 1, so first element in RLE (number of zeros) must be set to 0
        if(l.get(0))
            rle.add(0);

        // go over all elements and check if two consecutive are the same
        for (int i = 1; i < l.size(); i++) {
            if( l.get(i) != l.get(i - 1) )
            {
                rle.add(i - last_idx);
                last_idx = i;
            }
        }

        if(l.size() > 0){
            // handle last element of rle
            if( last_idx < l.size() - 1 )
                // last element is the same as one element before it - add number of these last elements
                rle.add(l.size() - last_idx);
            else
                // last element is different than one element before - add 1
                rle.add(1);
        }

        int[] rleArray = new int[rle.size()];
        for (int i = 0; i < rle.size(); i++) {
            rleArray[i] = rle.get(i);
        }
        return rleArray;
    }

    /*
     * (non-Javadoc)
     *
     * @see si.vicos.annotations.Annotation#reset()
     */
    @Override
    public void reset() {
          //reinitialize mask
    }

    /*
     * (non-Javadoc)
     *
     * @see si.vicos.annotations.Annotation#unpack(java.lang.String)
     */
    @Override
    public void unpack(String data) throws ParseException {
        if (StringUtils.empty(data))
            return;

        try {
            String lines[] = data.split("\\r?\\n");
            int width = lines.length;
            int height = lines[0].split(",").length;
            Boolean [][] pack = new Boolean[width][height];

            for (int i = 0; i < width; i++) {
                lines[i] = lines[i].replaceAll("\\[", "");
                lines[i] = lines[i].replaceAll("\\]", "");
                String[] line = lines[i].split(",");
                for (int j = 0; j < line.length; j++) {
                    if( line[j] == "true")
                        mask[i][j] = true;
                    else
                        mask[i][j] = false;
                }

            }


        } catch (NoSuchElementException e) {
            throw new ParseException("Unable to parse", -1);
        } catch (NumberFormatException e) {
            throw new ParseException("Unable to parse", -1);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see si.vicos.annotations.Annotation#clone()
     */
    @Override
    public Annotation clone() {
        return new SegmentationMaskAnnotation(mask);
    }

    /**
     * Changes the pixels value
     */
    private void set(int x, int y) {
        mask[y][x] = !mask[y][x];
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return Arrays.deepToString(this.mask).replace("], ", "]\n");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * si.vicos.annotations.Annotation#validate(si.vicos.annotations.Annotation)
     */
    @Override
    public boolean validate(Annotation a) {
        return a instanceof SegmentationMaskAnnotation;
    }

    /*
     * (non-Javadoc)
     *
     * @see si.vicos.annotations.Annotation#getType()
     */
    @Override
    public AnnotationType getType() {
        return AnnotationType.SEGMENTATION_MASK;
    }

    /*
     * (non-Javadoc)
     *
     * @see si.vicos.annotations.Annotation#canInterpolate()
     */
    @Override
    public boolean canInterpolate() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see si.vicos.annotations.Annotation#scale(float)
     */
    @Override
    public Annotation scale(float scale) throws UnsupportedOperationException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see si.vicos.annotations.ShapeAnnotation#getBoundingBox()
     */
    @Override
    public RectangleAnnotation getBoundingBox() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see si.vicos.annotations.Annotation#isNull()
     */
    @Override
    public boolean isNull() {
        for (int i = 0; i < mask.length; i++) {
            for (int j = 0; j < mask[i].length; j++) {
                if(mask[i][j])
                    return false;
            }
        }
        return true;
    }

    /**
     * Contains.
     *
     * @param a
     *            the a
     * @return true, if successful
     */
    public boolean contains(PointAnnotation a) {

        return mask[(int) a.getY()][(int) a.getX()];

    }


    @Override
    public Point2D getCenter() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see si.vicos.annotations.ShapeAnnotation#getPolygon()
     */
    @Override
    public List<Point2D> getPolygon() {
      return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * si.vicos.annotations.Annotation#convert(si.vicos.annotations.Annotation)
     */
    @Override
    public Annotation convert(Annotation a)
            throws UnsupportedOperationException {

        if (a instanceof RectangleAnnotation)
            return a;

        if (a instanceof ShapeAnnotation) {
            return ((ShapeAnnotation) a).getBoundingBox();
        }

        return super.convert(a);
    }

}