package net.cilib.cidb;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Stack;

/**
 *
 * @author Kristina
 */
public class ShuntingYardParser {

    Queue<String> queue;
    Stack<String> stack;

    public ShuntingYardParser() {
        queue = new ArrayDeque<String>();
        stack = new Stack<String>();
    }

    public ShuntingYardParser(ShuntingYardParser copy) {
        queue = copy.queue;
        stack = copy.stack;
    }

    public ShuntingYardParser getClone() {
        return new ShuntingYardParser();
    }

    public Queue<String> getSearchString(String input) {
        generateQueuedRepresentation(split(input));
        return queue;
    }

    private String[] split(String input) {
        String[] splitInput = input.split(" ");
        String toConcatinate = "";
        String[] tempSplit = splitInput.clone();
        int index = 0;
        int size = splitInput.length;
        int numberofOpenBrackets = 0;
        int numberofClosedBrackets = 0;

        while (index < size) {
            String value = splitInput[index];
            splitInput[index] = toConcatinate + value;
            tempSplit[index] = toConcatinate + value;

            toConcatinate = "";
            index++;

            if (value.contains("(")) {
                numberofOpenBrackets++;

                if (value.length() > 1) {
                    int indexForTemp = 0;
                    tempSplit = new String[splitInput.length + 1];

                    for (String other : splitInput) {
                        if (other.equals(value)) {
                            tempSplit[indexForTemp] = "(";
                            indexForTemp++;
                            tempSplit[indexForTemp] = value.substring(1);
                            indexForTemp++;
                            size++;
                        } else {
                            tempSplit[indexForTemp] = other;
                            indexForTemp++;
                        }

                    }
                }
            } else if (value.contains(")")) {
                numberofClosedBrackets++;

                if (value.length() > 1) {
                    int indexForTemp = 0;
                    tempSplit = new String[splitInput.length + 1];
                    for (String other : splitInput) {
                        String substringOther = other;
                        if (other.contains("!")) {
                            substringOther = other.substring(1);
                        }

                        if (substringOther.equals(value)) {
                            tempSplit[indexForTemp] = other.substring(0, other.length() - 1);
                            indexForTemp++;
                            tempSplit[indexForTemp] = ")";
                            indexForTemp++;
                            size++;
                            index++;
                        } else {
                            tempSplit[indexForTemp] = other;
                            indexForTemp++;
                        }

                    }
                }

            }

            splitInput = tempSplit.clone();

            if (value.equalsIgnoreCase("NOT")) {
                toConcatinate = "!";
                tempSplit = new String[splitInput.length - 1];
                int counter = 0;
                size--;
                index--;
                boolean hasChangedAlready = false;
                for (String newValue : splitInput) {
                    if (!newValue.equals(value) && !hasChangedAlready) {
                        tempSplit[counter] = newValue;
                        counter++;
                    } else if (hasChangedAlready) {
                        tempSplit[counter] = newValue;
                        counter++;
                    } else {
                        hasChangedAlready = true;
                    }
                }

                splitInput = tempSplit.clone();
            }


        }

        if (numberofClosedBrackets != numberofOpenBrackets) {
            Util.exit("There are unmatching brackets. Please ensure that for each opening bracket there is a closed bracket to match it.", -1);
        }

        return splitInput;
    }

    private void generateQueuedRepresentation(String[] input) {
        for (String item : input) {
            if (item.contains(":")) {
                queue.add(item);
            } else if (item.equalsIgnoreCase("(")) {
                stack.push(item);
            } else if (item.equalsIgnoreCase(")")) {
                String popedValue = stack.pop();
                while (!popedValue.equalsIgnoreCase("(")) {
                    queue.add(popedValue);
                    popedValue = stack.pop();
                }
            } else if (item.equalsIgnoreCase("AND") || item.equalsIgnoreCase("OR")) {
                if (!stack.isEmpty() && item.equalsIgnoreCase("OR") && stack.lastElement().equalsIgnoreCase("AND")) {
                    queue.add(stack.pop());
                }

                stack.push(item);
            }
        }

        while (!stack.isEmpty()) {
            queue.add(stack.pop());
        }

    }
}
