/*
 *  Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package org.openjdk.jextract.impl;

import org.openjdk.jextract.JextractTool;

import java.util.List;
import java.util.stream.Collectors;

public final class DoxygenCommentConverter {
    private DoxygenCommentConverter() {
    }

    public static String toJavaDoc(DoxygenCommentTree.FullCommentNode fullCommentNode) {
        JavaDocBuilder builder = new JavaDocBuilder();
        visitFullCommentNode(builder, fullCommentNode);
        return finish(builder.build());
    }

    private static String finish(String commentText) {
        return commentText.replace("*/", "*@/").lines()
            .map(line -> " * " + line + "\n")
            .collect(Collectors.joining());
    }

    private static void visitFullCommentNode(JavaDocBuilder builder, DoxygenCommentTree.FullCommentNode fullCommentNode) {
        for (DoxygenCommentTree.BlockContentCommentNode block : fullCommentNode.getBlocks()) {
            switch (block) {
                case DoxygenCommentTree.ParagraphCommentNode paragraphCommentNode -> visitParagraphNode(builder.getMain(), paragraphCommentNode);
                case DoxygenCommentTree.ParamCommandCommentNode paramCommandCommentNode -> visitParamCommandNode(builder.getBlockTags(), paramCommandCommentNode);
                case DoxygenCommentTree.VerbatimBlockCommentNode verbatimBlockCommentNode -> visitVerbatimBlockCommandNode(builder.getMain(), verbatimBlockCommentNode);
                case DoxygenCommentTree.VerbatimLineCommentNode verbatimLineCommentNode -> visitVerbatimLineNode(builder.getMain(), verbatimLineCommentNode);
                case DoxygenCommentTree.BlockCommandCommentNode blockCommandCommentNode -> visitBlockCommandNode(builder, blockCommandCommentNode);
            }
        }
    }

    private static void visitParagraphNode(StringBuilder builder, DoxygenCommentTree.ParagraphCommentNode paragraphCommentNode) {
        if (paragraphCommentNode.isWhitespace()) {
            return;
        }
        List<DoxygenCommentTree.InlineContentCommentNode> content = paragraphCommentNode.getContent();
        for (DoxygenCommentTree.InlineContentCommentNode inlineContent : content) {
            switch (inlineContent) {
                case DoxygenCommentTree.HTMLEndTagCommentNode htmlEndTagCommentNode -> visitHTMLEndTagNode(builder, htmlEndTagCommentNode);
                case DoxygenCommentTree.HTMLStartTagCommentNode htmlStartTagCommentNode -> visitHTMLStartTagNode(builder, htmlStartTagCommentNode);
                case DoxygenCommentTree.InlineCommandCommentNode inlineCommandCommentNode -> visitInlineCommandNode(builder, inlineCommandCommentNode);
                case DoxygenCommentTree.TextCommentNode textCommentNode -> visitTextNode(builder, textCommentNode);
            }
        }
        builder.append("\n");
    }

    private static void visitHTMLEndTagNode(StringBuilder builder, DoxygenCommentTree.HTMLEndTagCommentNode htmlEndTagCommentNode) {
        String tagName = upgradeHTML(htmlEndTagCommentNode.getTagName());
        builder.append("</%s> ".formatted(tagName));
    }

    private static void visitHTMLStartTagNode(StringBuilder builder, DoxygenCommentTree.HTMLStartTagCommentNode htmlStartTagCommentNode) {
        String tagName = upgradeHTML(htmlStartTagCommentNode.getTagName());
        builder.append(" <%s".formatted(tagName));
        htmlStartTagCommentNode.getAttributes().forEach((name, value) -> {
            builder.append(" %s=\"%s\"".formatted(name, value));
        });
        if (htmlStartTagCommentNode.isSelfClosing()) {
            builder.append("/>");
        } else {
            builder.append(">");
        }
    }

    private static void visitInlineCommandNode(StringBuilder builder, DoxygenCommentTree.InlineCommandCommentNode inlineCommandCommentNode) {
        String commandName = inlineCommandCommentNode.getCommandName();
        List<String> args = inlineCommandCommentNode.getArgs();

        switch (commandName) {
            case "b" -> appendInlineCommandAsHTML(builder, "strong", args);
            case "a", "e", "em" -> appendInlineCommandAsHTML(builder, "em", args);
            case "c", "p" -> appendInlineCommandAsInlineJavaDocTag(builder, "code", args);

            default -> {
                switch (inlineCommandCommentNode.getRenderKind()) {
                    case Anchor -> {}
                    case Bold -> appendInlineCommandAsHTML(builder, "strong", args);
                    case Emphasized -> appendInlineCommandAsHTML(builder, "em", args);
                    case Monospaced -> appendInlineCommandAsInlineJavaDocTag(builder, "code", args);
                    case Normal -> {
                        if (args.size() == 1) {
                            builder.append(" %s ".formatted(args.getFirst()));
                        } else {
                            if (JextractTool.DEBUG) {
                                System.out.printf("Expected one argument for inline command (%s)%n", args);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void visitTextNode(StringBuilder builder, DoxygenCommentTree.TextCommentNode textCommentNode) {
        builder.append(textCommentNode.getText().strip());
    }

    private static void visitParamCommandNode(StringBuilder builder, DoxygenCommentTree.ParamCommandCommentNode paramCommandCommentNode) {
        if (!paramCommandCommentNode.isParamIndexValid()) {
            if (JextractTool.DEBUG) {
                System.out.printf("Found invalid parameter (%s)%n", paramCommandCommentNode.getParamName());
            }
            return;
        }
        String paramName = paramCommandCommentNode.getParamName();
        String directionString;
        if (!paramCommandCommentNode.isDirectionExplicit()) {
            directionString = "";
        } else {
            directionString = switch (paramCommandCommentNode.getDirection()) {
                case In -> "[in] ";
                case Out -> "[out] ";
                case InOut -> "[in,out] ";
            };
        }
        StringBuilder paragraphBuilder = new StringBuilder();
        visitParagraphNode(paragraphBuilder, paramCommandCommentNode.getParagraph());
        String paragraph = paragraphBuilder.toString().strip();

        builder.append("@param %s %s%s".formatted(paramName, directionString, paragraph)).append("\n");
    }

    private static void visitVerbatimBlockCommandNode(StringBuilder builder, DoxygenCommentTree.VerbatimBlockCommentNode verbatimBlockCommentNode) {
        builder.append("{@snippet :\n");
        for (DoxygenCommentTree.VerbatimBlockLineCommentNode verbatimBlockLineCommentNode : verbatimBlockCommentNode.getLines()) {
            visitVerbatimBlockLineCommentNode(builder, verbatimBlockLineCommentNode);
            builder.append("\n");
        }
        builder.append("}\n");
    }

    private static void visitVerbatimBlockLineCommentNode(StringBuilder builder, DoxygenCommentTree.VerbatimBlockLineCommentNode verbatimBlockLineCommentNode) {
        builder.append(verbatimBlockLineCommentNode.getText().strip());
    }

    private static void visitVerbatimLineNode(StringBuilder builder, DoxygenCommentTree.VerbatimLineCommentNode verbatimLineCommentNode) {
        builder.append("{@code %s}%n".formatted(verbatimLineCommentNode.getText()));
    }

    private static void visitBlockCommandNode(JavaDocBuilder builder, DoxygenCommentTree.BlockCommandCommentNode blockCommandCommentNode) {
        List<String> args = blockCommandCommentNode.getArgs();
        String argsString;
        if (args.isEmpty()) {
            argsString = "";
        } else {
            argsString = String.join(" ", args) + " ";
        }
        StringBuilder paragraphBuilder = new StringBuilder();
        visitParagraphNode(paragraphBuilder, blockCommandCommentNode.getParagraph());
        String paragraph = paragraphBuilder.toString().strip();

        String commandName = blockCommandCommentNode.getCommandName();
        switch (commandName) {
            case "param" -> appendBlockCommandAsBlockJavaDocTag(builder.getBlockTags(), "param", argsString, paragraph);
            case "return", "returns", "result" -> appendBlockCommandAsBlockJavaDocTag(builder.getBlockTags(), "return", argsString, paragraph);
            case "throw", "throws", "exception" -> appendBlockCommandAsBlockJavaDocTag(builder.getBlockTags(), "throws", argsString, paragraph);
            case "see", "sa" -> appendBlockCommandAsBlockJavaDocTag(builder.getBlockTags(), "see", argsString, paragraph);
            case "deprecated" -> appendBlockCommandAsBlockJavaDocTag(builder.getBlockTags(), "deprecated", argsString, paragraph);
            case "since" -> appendBlockCommandAsBlockJavaDocTag(builder.getBlockTags(), "since", argsString, paragraph);
            case "author", "authors" -> appendBlockCommandAsBlockJavaDocTag(builder.getBlockTags(), "author", argsString, paragraph);
            case "noop" -> appendBlockCommandAsBlockJavaDocTag(builder.getBlockTags(), "hidden", argsString, paragraph);

            case "brief", "short" -> appendBlockCommandAsInlineJavaDocTag(builder.getMain(), "summary", argsString, paragraph);

            default -> builder.appendEndMain("<p><b>%s</b>: %s%s</p>%n".formatted(commandName, argsString, paragraph));
        }
    }

    private static String upgradeHTML(String htmlTag) {
        return switch (htmlTag) {
            case "tt" -> "code";
            case "strike" -> "s";
            default -> htmlTag;
        };
    }

    private static void appendInlineCommandAsHTML(StringBuilder builder, String htmlTag, List<String> args) {
        if (args.size() == 1) {
            builder.append(" <%1$2s>%2$2s</%1$2s> ".formatted(htmlTag, args.getFirst()));
        } else {
            if (JextractTool.DEBUG) {
                System.out.printf("Expected one argument for inline command (%s)%n", args);
            }
        }
    }

    private static void appendInlineCommandAsInlineJavaDocTag(StringBuilder builder, String javaDocTag, List<String> args) {
        if (args.size() == 1) {
            builder.append(" {@%s %s} ".formatted(javaDocTag, args.getFirst()));
        } else {
            if (JextractTool.DEBUG) {
                System.out.printf("Expected one argument for inline command (%s)%n", args);
            }
        }
    }

    private static void appendBlockCommandAsBlockJavaDocTag(StringBuilder builder, String javaDocTag, String argsString, String paragraph) {
        builder.append("@%s %s%s%n".formatted(javaDocTag, argsString, paragraph));
    }

    private static void appendBlockCommandAsInlineJavaDocTag(StringBuilder builder, String javaDocTag, String argsString, String paragraph) {
        builder.append("{@%s %s%s}%n".formatted(javaDocTag, argsString, paragraph));
    }
}
