// Generated by jextract

package org.openjdk;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
/**
 * {@snippet :
 * void (*JImageClose_t)(struct JImageFile* jimage);
 * }
 */
public interface JImageClose_t {

    void apply(java.lang.foreign.MemorySegment jimage);
    static MemorySegment allocate(JImageClose_t fi, Arena scope) {
        return RuntimeHelper.upcallStub(JImageClose_t.class, fi, constants$0.JImageClose_t$FUNC, scope);
    }
    static JImageClose_t ofAddress(MemorySegment addr, Arena arena) {
        MemorySegment symbol = addr.reinterpret(arena.scope(), null);
        return (java.lang.foreign.MemorySegment _jimage) -> {
            try {
                constants$0.JImageClose_t$MH.invokeExact(symbol, _jimage);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


