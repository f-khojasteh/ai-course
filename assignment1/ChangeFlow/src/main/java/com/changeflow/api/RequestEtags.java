package com.changeflow.api;

import com.changeflow.application.*;
import java.util.*;
import java.util.regex.Pattern;
import static com.changeflow.application.ApplicationFailure.Code.*;

/** Transport-only parsing. Failed strong comparisons are evaluated after Application access checks. */
public final class RequestEtags {
    private static final Pattern TAG = Pattern.compile("\"cr-([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})-v([0-9]+)\"");
    private static final Pattern WEAK = Pattern.compile("W/\"[\\x21\\x23-\\x7e\\x80-\\xff]*\"");
    private RequestEtags() { }

    public static String value(RequestSnapshot snapshot) {
        return "\"cr-" + snapshot.request().id() + "-v" + snapshot.revision() + "\"";
    }

    public static long expected(UUID routeId, List<String> headers) {
        if (headers == null || headers.isEmpty()) throw new ApplicationFailure(PRECONDITION_REQUIRED);
        if (headers.size() != 1) throw new ApplicationFailure(UNSUPPORTED_IF_MATCH);
        String text = headers.get(0).strip();
        if (text.equals("*")) throw new ApplicationFailure(UNSUPPORTED_IF_MATCH);
        // Negative is an impossible stored revision, not an immediate HTTP failure:
        // the service must protect visibility and action permissions first.
        if (WEAK.matcher(text).matches()) return -1;
        if (text.contains(",")) throw new ApplicationFailure(UNSUPPORTED_IF_MATCH);
        var match = TAG.matcher(text);
        if (!match.matches()) throw new ApplicationFailure(INVALID_IF_MATCH);
        try {
            UUID itemId = UUID.fromString(match.group(1));
            long revision = Long.parseLong(match.group(2));
            String canonical = "\"cr-" + routeId + "-v" + revision + "\"";
            return routeId.equals(itemId) && text.equals(canonical) ? revision : -1;
        } catch (IllegalArgumentException invalid) { throw new ApplicationFailure(INVALID_IF_MATCH); }
    }
}
