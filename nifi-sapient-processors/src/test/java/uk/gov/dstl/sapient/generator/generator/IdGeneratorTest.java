package uk.gov.dstl.sapient.generator.generator;

import org.junit.Test;

import static org.junit.Assert.*;

public class IdGeneratorTest {

    @Test
    public void testDeterministicUlidIsStable() {
        String id1 = IdGenerator.deterministicUlid("test-seed");
        String id2 = IdGenerator.deterministicUlid("test-seed");
        assertEquals("Same seed should produce same ULID", id1, id2);
    }

    @Test
    public void testDifferentSeedsProduceDifferentIds() {
        String id1 = IdGenerator.deterministicUlid("seed-alpha");
        String id2 = IdGenerator.deterministicUlid("seed-bravo");
        assertNotEquals("Different seeds should produce different ULIDs", id1, id2);
    }

    @Test
    public void testDeterministicUlidHasCorrectLength() {
        String id = IdGenerator.deterministicUlid("any-seed");
        assertEquals("ULID should be 26 characters", 26, id.length());
    }

    @Test
    public void testRandomUlidIsUnique() {
        String id1 = IdGenerator.randomUlid();
        String id2 = IdGenerator.randomUlid();
        assertNotEquals("Random ULIDs should be unique", id1, id2);
    }

    @Test
    public void testRandomUlidHasCorrectLength() {
        String id = IdGenerator.randomUlid();
        assertEquals("ULID should be 26 characters", 26, id.length());
    }
}
