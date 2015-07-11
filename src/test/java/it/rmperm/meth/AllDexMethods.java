package it.rmperm.meth;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class AllDexMethods {


    @Test
    public void testEquals() throws Exception {
        String def = "def";
        String nam = "nam";
        List<String> pars = new ArrayList<>();
        pars.add("a");
        pars.add("b");
        pars.add("c");
        String ret = "ret";

        DexMethod dm = new DexMethod(def, nam, pars, ret);
        DexMethod dm2 = new DexMethod(def, nam, pars, ret);
        DexPermMethod dpm = new DexPermMethod(def, nam, pars, ret, "perm");
        DexPermMethod dpm2 = new DexPermMethod(def, nam, pars, ret, "perm");

        // base case
        assertTrue(dm.equals(dm2));
        assertTrue(dpm.equals(dpm2));

        // symmetric
        assertTrue(dm.equals(dpm));
        assertTrue(dpm.equals(dm));

        // reflexive
        assertTrue(dm.equals(dm));
        assertTrue(dpm.equals(dpm));

        // null -> false
        assertFalse(dm.equals(null));
        assertFalse(dpm.equals(null));

        // transitive
        assertTrue(dm.equals(dpm));
        assertTrue(dpm.equals(dm2));
        assertTrue(dm.equals(dm2));

    }
}