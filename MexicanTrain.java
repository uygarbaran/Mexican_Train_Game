import java.util.*;
import java.io.*;
import java.lang.*;

// In hexadecimal -> 0 to c (12)
// If more than 3, print last 3. If less, print whatever is in the hashmap. When you print,
// do it in this format so it is easy to find:
// FORMAT: max number is x
// <number>: tiles...

public class MexicanTrain
{
	public static final Scanner s = new Scanner(System.in);
	public static HashMap<String, LinkedList<String>> states; // <current domino chain, dominoes left in hand>
	public static int numDomino, maxDominoes;
	public static Queue<String> q;
	public static LinkedList<String> userFirstHand;

	public static void main(String [] args)
	{
		System.out.print("\nNumber of dominoes in your hand: ");
		numDomino = Integer.parseInt(s.next().trim());
		System.out.println("Please type your hand in hexadecimal format (order of individual dominoes does not matter) as follows: 00-12-3c-ab-c9-bb (in decimal, this is [0,0], [1,2], [3,12], [10,11], [12,9], [11,11])");
		String initialHand[] = s.next().trim().split("-"); // Split the string into substrings of dominoes

		LinkedList<String> hand = new LinkedList<>();
		StringBuilder builder;

		// Add the hand in a linked list.
		for (int i = 0; i < initialHand.length; i++)
			hand.add(initialHand[i]);

		userFirstHand = new LinkedList<String>(hand); // Additional copy of user's initial hand for printing in the output document

		// Ask if it is their turn in the go function. If so, put the duplicate dominoes in the queue and state and start. Otherwise, ask what is in the middle and start by putting whatever is possible for that one.
		// Note that you will have to overwrite the Hashset's equals function to find the necessary thing just by putting single number.
		String input;
		do {
			System.out.println("Are you starting the game (y/n)?");
			input = s.next().trim();
		} while (!input.equalsIgnoreCase("yes") && !input.equalsIgnoreCase("y") && !input.equalsIgnoreCase("n") && !input.equalsIgnoreCase("no"));

		go(input, hand);
	}

	public static boolean isDouble(String str)
	{
		return (str.charAt(0) == str.charAt(1));
	}

	// Reverses the domino tile and returns the result.
	private static String reverseString(String str)
	{
		StringBuilder builder = new StringBuilder(2);
		builder.append(str.charAt(1));
		builder.append(str.charAt(0));
		return builder.toString();
	}

	public static int go(String isStarting, LinkedList<String> initialHand)
	{
		states = new HashMap<>();
		q = new ArrayDeque<>();
		String startingDomino, nextDomino;
		boolean noMoves = true; // Currently, no moves are possible/have been made.
		maxDominoes = 0;

		// Input the first state.
		//states.put("", new LinkedList<String>(initialHand));

		// If they are starting the game...
		if (isStarting.equalsIgnoreCase("yes") || isStarting.equalsIgnoreCase("y"))
		{
			// Find all the doubles and put them into the queue.
			for (int i = 0; i < initialHand.size(); i++)
			{
				nextDomino = initialHand.get(i);

				// If the domino tile is a double...
				if (MexicanTrain.isDouble(nextDomino))
				{
					noMoves = false; // There will be at least one move
					q.add(nextDomino); // Add the move to the queue
					initialHand.remove(i); // Remove the tile from your hand
					states.put(nextDomino, new LinkedList<String>(initialHand)); // Add the state of <current domino chain, dominos left in hand after the move>
					maxDominoes = Math.max(maxDominoes, nextDomino.length()/2); // Update the maxDominoes count
					initialHand.addFirst(nextDomino); // Redo what you did for other states to be able to use this tile, but insert to head to never visit again
				}
			}
		}
		else // If they aren't starting the game...
		{
			// Ask them what is in the middle. Then put everything that could match to that.
			System.out.println("What is the starting domino tile (format: <two same characters between 0 and c>)?");
			startingDomino = s.next().trim();

			// Find all possible tiles that could be placed after the initial domino tile.
			for (int i = 0; i < initialHand.size(); i++)
			{
				nextDomino = initialHand.get(i);

				// If the domino tiles can go one after the other...
				if (MexicanTrain.canGoNext(startingDomino, nextDomino))
				{
					noMoves = false;
					String next = turnDominoIfNecessary(startingDomino, nextDomino);
					q.add(next);
					initialHand.remove(i);
					states.put(next, new LinkedList<String>(initialHand));
					maxDominoes = Math.max(maxDominoes, next.length()/2); // Update the maxDominoes count
					initialHand.addFirst(next);
				}
			}
		}

		if (!noMoves)
		{
			// Run bfs.
			while (!q.isEmpty())
			{
				String cur = q.remove(); // Get the current state/domino chain.
				// System.out.println("current state: " + cur);
				ArrayList<MexicanTrainState> nextMoves = getNextMoves(cur, states.get(cur));

				for (MexicanTrainState next : nextMoves)
				{
					if (!states.containsKey(next.currState))
					{
						states.put(next.currState, new LinkedList<String>(next.currHand));
						q.add(next.currState);
						maxDominoes = Math.max(maxDominoes, next.currState.length()/2); // Update the maxDominoes count
					}
				}
			}
		}

		// All the possible moves have been made and their states have been stored. Print the results for the user.
		try {
			PrintStream outFile = new PrintStream(new File("results.txt"));
			outFile.println();
			outFile.print("Your initial hand: ");

			for (int i = 0; i < userFirstHand.size(); i++)
			{
				String str = userFirstHand.get(i);
				outFile.print("[" + MexicanTrain.hexToDecimal(str.charAt(0)) + ",");
				outFile.print(MexicanTrain.hexToDecimal(str.charAt(1)) + "]");

				if (i < userFirstHand.size() - 1)
					outFile.print("-");
			}

			outFile.println();
			outFile.println("\nMaximum number of dominoes placed: " + maxDominoes);
			outFile.println();
			outFile.println("The results:");
			outFile.println("Format: <number of dominoes placed> dominoes: <domino chain...>");
			outFile.println();

			// Showing every step would make the file very long. Only show the states where the dominoes placed are within the range of (maxDominoes - 3) to (maxDominoes)
			// If maxDominoes is less than 3, just show from 0 to maxDominoes.
			int minNumToShow = (maxDominoes < 3) ? 0 : maxDominoes - 3;

			// Get the list of keys (all the moves possible).
			ArrayList<String> listOfKeys = new ArrayList<String>(states.keySet());

			// Sort the list by the number of moves.
			Collections.sort(listOfKeys,  Collections.reverseOrder(Comparator.comparing(String::length)));

			for (String domChain : listOfKeys)
			{
				if ((domChain.length()/2) < minNumToShow)
					continue;

				outFile.print((domChain.length()/2) + " dominoes: ");

				int strIndex = 0;

				do {
					outFile.print("[" + MexicanTrain.hexToDecimal(domChain.charAt(strIndex)) + ",");
					strIndex++;
					outFile.print(MexicanTrain.hexToDecimal(domChain.charAt(strIndex)) + "]");
					strIndex++;

					if (strIndex < (domChain.length() - 1))
						outFile.print("-");

				} while (strIndex < domChain.length() - 1);

				outFile.println();
			}

			outFile.close();
		}
		catch (FileNotFoundException e)
		{
			System.out.println("Unable to open the output file for writing. Please try again.");
		}

		return 0;
	}

	// Returns a string which converts a tile number in hexadecimal to decimal (goes from 0 to c which is 0 to 12).
	private static String hexToDecimal(char num)
	{
		if (num >= '0' && num <= '9')
			return String.valueOf(num);

		if (num == 'a')
			return new String("10");
		else if (num == 'b')
			return new String("11");

		// This means num is c, which is 12.
		return new String("12");
	}

	// Given two domino tiles, returns whether the second one can go after the first one.
	public static boolean canGoNext(String placedDomino, String nextDomino)
	{
		char a1, a2, lastNum;
		a1 = nextDomino.charAt(0);
		a2 = nextDomino.charAt(1);
		lastNum = placedDomino.charAt(1); // This is the latest number on the board

		return (lastNum == a1 || lastNum == a2);
	}

	// If necessary, turns the next domino to be able to attach to the placed domino on the board.
	// Assumes that the domino tiles are already attachable.
	private static String turnDominoIfNecessary(String placedDomino, String nextDomino)
	{
		char lastNum = placedDomino.charAt(1); // This is the number next domino is trying to attach to.
		char b1 = nextDomino.charAt(0);

		// If the next domino is already aligned correctly, just return as it is.
		if (b1 == lastNum)
			return nextDomino;

		// Else, reverse it.
		return MexicanTrain.reverseString(nextDomino);
	}

	// Returns the last domino out of the whole domino chain. Assumes there is at least 1 domino.
	private static String getLastDomino(String dominoChain)
	{
		int len = dominoChain.length();
		StringBuilder builder = new StringBuilder(2);
		builder.append(dominoChain.charAt(len - 2));
		builder.append(dominoChain.charAt(len - 1));
		return builder.toString();
	}

	// Given the current state of dominoes the user put on the board and the current hand, returns all the possible next moves.
	public static ArrayList<MexicanTrainState> getNextMoves(String dominoChain, LinkedList<String> curHand)
	{
		ArrayList<MexicanTrainState> nextMoves = new ArrayList<>();
		MexicanTrainState st;

		// There should be at least two characters since any domino has at least 2 numbers.
		if (dominoChain.length() <= 1)
			return nextMoves;

		String lastDomino = getLastDomino(dominoChain); // The last domino in the chain is what matters

		// Go through the current hand and collect all possible next moves in the nextMoves array list.
		for (int i = 0; i < curHand.size(); i++)
		{
			String nextDomino = curHand.get(i);

			// If the domino tiles can go one after the other...
			if (MexicanTrain.canGoNext(lastDomino, nextDomino))
			{
				String next = turnDominoIfNecessary(lastDomino, nextDomino);
				curHand.remove(i); // Remove the next possible tile from hand temporarily.

				// Add the next domino tile to the domino chain to fully represent the unique state (first parameter).
				st = new MexicanTrainState(dominoChain.concat(next), new LinkedList<String>(curHand)); // Create the next state

				nextMoves.add(st); // Add the next state to next move array list
				curHand.addFirst(next); // Revert the change in your current hand for others moves to be able to utilize the domino tile later on.
			}
		}

		return nextMoves;
	}
}

// Improvement Note: It would be easier if your bfs queue held this class rather than String class. Then it would be able to return both the currState and currHand.
class MexicanTrainState
{
	public String currState;
	public LinkedList<String> currHand;

	public MexicanTrainState(String state, LinkedList hand)
	{
		currState = new String(state);
		currHand = hand;
	}
}
