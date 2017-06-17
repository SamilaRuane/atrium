package ch.tutteli.atrium.reporting

import ch.tutteli.atrium.assertions.*
import ch.tutteli.atrium.reporting.translating.ITranslatable
import ch.tutteli.atrium.reporting.translating.ITranslator
import ch.tutteli.kbox.appendToStringBuilder

/**
 * Formats an [IAssertion] and its [Message]s, putting each message on its own line.
 *
 * Currently the following [IAssertion] types are supported:
 * - [IAssertionGroup] with the following types:
 *   - [RootAssertionGroupType]
 *   - [FeatureAssertionGroupType]
 * - [IAssertion]
 *
 * and the following [Message] types:
 * - [IOneMessageAssertion]
 *
 * @property objectFormatter Used to format objects such as [Message.representation].
 * @property translator Used to translate [ITranslatable]s such as [Message.description].
 *
 * @constructor
 * @param objectFormatter Used to format objects such as [Message.representation].
 * @param translator Used to translate [ITranslatable]s such as [Message.description].
 */
internal class SameLineAssertionFormatter(
    private val objectFormatter: IObjectFormatter,
    private val translator: ITranslator
) : IAssertionFormatter {

    override fun format(sb: StringBuilder, assertion: IAssertion, assertionFilter: (IAssertion) -> Boolean, messageFilter: (Message) -> Boolean) {
        format(assertion, MethodObject(0, sb, assertionFilter, messageFilter))
    }

    private fun format(assertion: IAssertion, methodObject: MethodObject) {
        if (methodObject.assertionFilter(assertion)) {
            appendIndent(methodObject)
            when (assertion) {
                is IAssertionGroup -> formatGroup(assertion, methodObject)
                is IOneMessageAssertion -> appendMessage(assertion.message, methodObject)
                else -> basicFormat(assertion, methodObject)
            }
        }
    }

    private fun formatGroup(assertionGroup: IAssertionGroup, methodObject: MethodObject) = when (assertionGroup.type) {
        is FeatureAssertionGroupType -> formatFeature(assertionGroup, methodObject)
        //everything else are treated like RootAssertion
        else -> formatRootGroup(assertionGroup, methodObject)
    }

    private fun formatRootGroup(rootAssertionGroup: IAssertionGroup, methodObject: MethodObject) {
        methodObject.sb
            .appendPair(translator.translate(rootAssertionGroup.name), rootAssertionGroup.subject)
            .appendln()
            .appendAssertions(rootAssertionGroup.assertions, methodObject, { methodObject })
    }

    private fun formatFeature(featureAssertionGroup: IAssertionGroup, methodObject: MethodObject) {
        methodObject.sb
            .appendPair("-> " + translator.translate(featureAssertionGroup.name), featureAssertionGroup.subject)
            .appendln()
            .appendAssertions(featureAssertionGroup.assertions, methodObject, methodObject::newWithIncrementedMessageLevel)
    }

    private fun appendMessage(message: Message, methodObject: MethodObject) {
        if (methodObject.messageFilter(message)) {
            methodObject.sb.appendPair(translator.translate(message.description), message.representation)
        }
    }

    private fun basicFormat(assertion: IAssertion, methodObject: MethodObject) {
        methodObject.sb.append("Unsupported type ${assertion::class.java.name}, can only report whether it holds: ")
            .append(assertion.holds())
    }

    private fun appendIndent(methodObject: MethodObject) {
        for (i in 0 until methodObject.messageGroupLevel * NUMBER_OF_INDENT_SPACES) {
            methodObject.sb.append(' ')
        }
    }

    private fun StringBuilder.appendPair(left: String, right: Any?)
        = append(left).append(": ").append(objectFormatter.format(right))

    private fun StringBuilder.appendAssertions(assertions: List<IAssertion>, methodObject: MethodObject, factoryMethod: () -> MethodObject): StringBuilder {
        assertions
            .filter(methodObject.assertionFilter)
            .appendToStringBuilder(methodObject.sb, SEPARATOR) { it, _ ->
                format(it, factoryMethod())
            }
        return this
    }

    companion object {
        val SEPARATOR: String = System.getProperty("line.separator")!!
        internal val NUMBER_OF_INDENT_SPACES = 3
    }

    private class MethodObject(
        val messageGroupLevel: Int,
        val sb: StringBuilder,
        val assertionFilter: (IAssertion) -> Boolean,
        val messageFilter: (Message) -> Boolean) {

        fun newWithIncrementedMessageLevel(): MethodObject {
            return MethodObject(messageGroupLevel + 1, sb, assertionFilter, messageFilter)
        }
    }

}


