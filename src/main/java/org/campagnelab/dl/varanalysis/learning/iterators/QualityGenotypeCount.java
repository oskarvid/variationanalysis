package org.campagnelab.dl.varanalysis.learning.iterators;

/**
 * Created by rct66 on 6/1/16.
 */
public class QualityGenotypeCount extends GenotypeCount {

    int forwardCount;
    int reverseCount;
    String toSequence;
    float qualityScore;

    public QualityGenotypeCount(int forwardCount, int reverseCount, String toSequence, float qualityScore) {
        super(forwardCount, reverseCount, toSequence);
        this.qualityScore = qualityScore;
    }


    public int totalCount() {
        return forwardCount + reverseCount;
    }

    public float getQualityScore(){
        return qualityScore;
    }

    @Override
    public int compareTo(GenotypeCount o) {
        return o.totalCount() - totalCount();
    }

    @Override
    public String toString() {
        return String.format("totalCount=%d %d on + / %d on - %s, quality=%e",totalCount(), forwardCount,reverseCount,toSequence,qualityScore);
    }

}
