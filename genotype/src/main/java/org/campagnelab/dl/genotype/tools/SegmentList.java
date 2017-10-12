package org.campagnelab.dl.genotype.tools;

import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;
import org.campagnelab.goby.baseinfo.SequenceSegmentInformationWriter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Segment list for SSI.
 *
 * @author manuele
 */
public class SegmentList implements Iterable<SegmentList.Segment>{

    private final Function<Segment, Segment> function;
    private final SequenceSegmentInformationWriter writer;
    private Segment currentSegment;
    private int currentLastPosition = 0;
    private int currentLastReferenceIndex = 0;
    private String currentLastReferenceId = "";


    protected SegmentList(BaseInformationRecords.BaseInformation from, SequenceSegmentInformationWriter writer, Function<Segment, Segment> function) {
        this.newSegment(from);
        this.function = function;
        this.writer = writer;
    }


    public int getCurrentLocation() {
        return currentLastPosition;
    }

    /**
     * Opens a new segment from the record.
     * @param from
     */
    public void newSegment(BaseInformationRecords.BaseInformation from) {
        if (currentSegment != null)
            this.closeSegment();
        currentSegment = new Segment(from);
        this.add(from);

    }

    public void closeSegment() {
        if (this.function != null) {
            Segment processed = this.function.apply(currentSegment);
            processed.flush(writer);
            System.out.println(processed);
        } else {
            currentSegment.flush(writer);
            System.out.println(currentSegment);
        }
    }

    public void add(BaseInformationRecords.BaseInformation record) {
       currentSegment.add(record);
       setAsLast(record);
    }

    /**
     * Sets the record as the last one in the current segment.
     * @param record
     */
    private void setAsLast(BaseInformationRecords.BaseInformation record) {
        currentLastPosition = record.getPosition();
        currentLastReferenceIndex = record.getReferenceIndex();
        currentLastReferenceId = record.getReferenceId();
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<Segment> iterator() {
        return null;
    }

    public void close() {
        this.closeSegment();
        this.printStats();
    }

    private void printStats() {
        
    }

    /**
     * Holds the current open segment before it is stored in the list.
     */
    public class Segment {
        private int startPosition = 0;
        private int endPosition = 0;
        List<BaseInformationRecords.BaseInformation> records = new ArrayList<>();

        Segment(BaseInformationRecords.BaseInformation first) {
            System.out.println("Open a new segment at position " + Integer.toString(first.getPosition()));
            this.startPosition = first.getPosition();
            this.endPosition = first.getPosition();
            this.records.add(first);
        }

        protected void flush(SequenceSegmentInformationWriter writer) {
            //if (builder != null) {
                //close the previous segment
                System.out.println("Close the segment.");
                 /*try {
                   writer.appendEntry(builder.build());
                    //set the current* as end position
                    SegmentInformationRecords.ReferencePosition.Builder refBuilder = SegmentInformationRecords.ReferencePosition.newBuilder();
                    refBuilder.setLocation(currentLastPosition);
                    refBuilder.setReferenceIndex(currentLastReferenceIndex);
                    refBuilder.setReferenceId(currentLastReferenceId);
                    builder.setEndPosition(refBuilder);
                    printSegmentStats();
                } catch (IOException e) {
                    System.err.println("Unable to close the previous segment");
                    e.printStackTrace();
                } finally {
                    builder = null;
                    currentLastPosition = 0;
                    currentLastReferenceId = "";
                    currentLastReferenceIndex = 0;
                }   */
                //create statistics here.
            //}
        }


        /**
         * Adds a record to the current segment
         * @param record
         */
        protected void add(BaseInformationRecords.BaseInformation record) {
            record.getSamplesList().forEach(sampleInfo -> {
                        /*SegmentInformationRecords.Sample.Builder sampleBuilder = SegmentInformationRecords.Sample.newBuilder();
                        SegmentInformationRecords.Base.Builder baseBuilder = SegmentInformationRecords.Base.newBuilder();
                        //TODO: set real values here
                        baseBuilder.addFeatures(1f);
                        baseBuilder.addLabels(2f);
                        baseBuilder.addTrueLabel("foo");
                        sampleBuilder.addBase(baseBuilder);
                        builder.addSample(sampleBuilder);*/
                    }
            );
            /*SegmentInformationRecords.ReferencePosition.Builder refBuilder = SegmentInformationRecords.ReferencePosition.newBuilder();
            refBuilder.setLocation(record.getPosition());
            refBuilder.setReferenceIndex(record.getReferenceIndex());
            refBuilder.setReferenceId(record.getReferenceId()); */
            this.records.add(record);
            this.endPosition = record.getPosition();
        }

        @Override
        public String toString() {
            return "Segment{" +
                    "startPosition=" + startPosition +
                    ", endPosition=" + endPosition +
                    ", length=" + (endPosition - startPosition + 1) +
                    '}';
        }
    }
}
