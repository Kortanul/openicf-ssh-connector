/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */


package org.forgerock.openicf.connectors.ssh

import expect4j.Closure
import expect4j.Expect4j
import expect4j.ExpectState
import expect4j.matches.Match
import expect4j.matches.GlobMatch
import expect4j.matches.RegExpMatch
import expect4j.matches.TimeoutMatch
import expect4j.matches.EofMatch


/**
 * E4JWrapper basically wraps calls to underlying expect4j into Groovy Closures.
 * These closures are injected into the scripts as bindings.
 */
class E4JWrapper {

    final expect4j

    static final char ctrlC = 0x03;
    static final char ctrlD = 0x04;

    def E4JWrapper(Expect4j expect4j) {
        this.expect4j = expect4j
    }

    /**
     * Closure to redefine the Expect4J global default timeout
     */
    def globalTimeout = { expect4j.setDefaultTimeout((long) it * 1000) }

    /**
     * Closure to send Ctrl+C key combination
     */
    def sendControlC = { expect4j.send(new String(ctrlC)) }

    /**
     * Closure to send Ctrl+D key combination
     */
    def sendControlD = { expect4j.send(new String(ctrlD)) }

    /**
     * Closure to send the input to the remote host
     */
    def sender = { expect4j.send(it) }

    /**
     * Closure to send input with carriage return included
     */
    def senderln = { expect4j.send(it + "\r") }

    /**
     * Simple Wrapper to build a GlobalMatch pattern pair
     */
    def global = { String pattern, groovy.lang.Closure closure ->
        if (closure == null){
            new GlobMatch(pattern, null)
        }
        else {
            new GlobMatch(pattern, new Closure() {
                @Override
                public void run(ExpectState state) throws Exception {
                    closure(state)
                }
            })
        }
    }

    /**
     * Simple Wrapper to build a RegExpMatch pattern pair
     */
    def regexp = { String pattern, groovy.lang.Closure closure ->
        if (closure == null){
            new RegExpMatch(pattern, null)
        }
        else {
            new RegExpMatch(pattern, new Closure() {
                @Override
                public void run(ExpectState state) throws Exception {
                    closure(state)
                }
            })
        }
    }

    /**
     * Simple Wrapper to build a TimeoutMatch pattern pair
     */
    def timeout = { int milli, groovy.lang.Closure closure ->
        if (closure == null){
            new RegExpMatch(pattern, null)
        }
        else {
            new TimeoutMatch(milli as Long, new Closure() {
                @Override
                public void run(ExpectState state) throws Exception {
                    closure(state)
                }
            })
        }
    }

    /**
     * Simple Wrapper to build an EofMatch pattern pair
     */
    def eof = { groovy.lang.Closure closure ->
        new EofMatch(new Closure() {
            @Override
            public void run(ExpectState state) throws Exception {
                closure(state)
            }
        })
    }

    /**
     * The expectGlobal can be called with either:
     * - a simple String argument (Global match)
     * - a String argument and a Closure. The ExpectState is passed to the Closure
     * - a Match (global or regexp)
     * - a List of Match
     */
    def expectGlobal = { Object... arg ->
        def match = new ArrayList<Match>()

        if (arg.length == 1) {
            if(arg[0] instanceof String || arg[0] instanceof GString) {
                match.add(new GlobMatch(arg[0], null))
            }
            else if(arg[0] instanceof Match) {
                match.add(arg[0])
            }
            else if(arg[0] instanceof List) {
                match.addAll(arg[0])
            }
            else {
                throw new IllegalArgumentException("expect was called with a bad argument[0] type: " + arg[0].class.name)
            }
        }
        else if (arg.length == 2
                && (arg[0] instanceof String || arg[0] instanceof GString)
                && arg[1] instanceof groovy.lang.Closure) {
            final groovy.lang.Closure cl = (groovy.lang.Closure) arg[1]
            match.add(new GlobMatch(arg[0], new Closure() {
                @Override
                public void run(ExpectState state) throws Exception {
                    cl(state)
                }
            }))
        }
        else {
            throw new IllegalArgumentException("expect was called with a bad argument type: " + arg[0].class.name)
        }

        status(expect4j.expect(match as List<Match>))
    }

    /**
     * Translates E4J return codes
     * @param code
     * @return
     */
    def status(int code) {
        switch (code) {
        // the end of file marker was encountered when accessing the reader stream
            case Expect4j.RET_EOF:
                break
        // the timeout value expired prior to finding a concluding match
            case Expect4j.RET_TIMEOUT:
                break
        // some unforeseen condition occurred.
            case Expect4j.RET_UNKNOWN:
                break
        //no match was found and no re-attempt was made
            case Expect4j.RET_TRIED_ONCE:
                break
            default:
                break
        }
        return code
    }
}
