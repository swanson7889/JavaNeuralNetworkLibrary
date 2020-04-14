import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Scanner;


public class NN1 {

	private double[][][] weights;
	private double[][] biases;
	private int[] structure;

	private double[] input;
	private double[][] activations;

	private double[][][] dcdw;
	private double[][] dcdb;
	private double[][] dcda;

	private int trainedOn;
	private double best;

	//Init
	public NN1(int[] structure) {
		this.structure = structure;
		if(structure.length <= 1) {
			System.err.println("NN Structure must be at least 2 layers");
		}
		weights = new double[structure.length-1][][];
		biases = new double[structure.length-1][];
		activations = new double[structure.length-1][];
		for(int layer = 0; layer < structure.length-1; layer++) {
			if(structure[layer] <= 0 || structure[layer+1] <= 0) {
				System.err.println("NN Structure cannot include any numbers less than 1");
				return;
			}
			weights[layer] = randArr(structure[layer+1], structure[layer]);
			biases[layer] = randArr(structure[layer+1]);
			activations[layer] = new double[structure[layer+1]];
		}
		trainedOn = 0;
	}
	public NN1() { //Test Init
		load("Current.txt");

	}
	private double[][] randArr(int h, int w){
		double[][] rv = new double[h][w];
		for(int i = 0; i < h; i++)
			rv[i] = randArr(rv[i].length);
		return rv;
	}
	private double[] randArr(int len) {
		double[] rv = new double[len];
		for(int i = 0; i < rv.length; i++)
			rv[i] = Math.random()*2-1;
		return rv;
	}
	private double[][][] emptyWArr(int[] struc){
		double[][][] rv = new double[struc.length-1][][];
		for(int i = 0; i < struc.length-1; i++) {
			rv[i] = new double[struc[i+1]][struc[i]];
		}
		return rv;
	}
	private double[][] emptyBArr(int[] struc){
		double[][] rv = new double[struc.length-1][];
		for(int i = 0; i < struc.length-1; i++) {
			rv[i] = new double[struc[i+1]];
		}
		return rv;
	}
	//Testing
	public double[][] getStats(LinkedList<Image> images) {
		int[] total = new int[10];
		int successes = 0;
		int t = images.size();
		int[] correct = new int[10];
		NN1 nn = new NN1();
		for(Image image: images) {
			total[image.label] += 1;
			if(nn.classify(image)==image.label) {
				correct[image.label] += 1;
				successes++;
			}
		}
		double[] stats = new double[10];
		for(int i = 0; i < 10; i++) {
			stats[i] = correct[i]/(double)total[i]*100;
		}
		double d = successes/(double)t;
		return new double[][]{new double[] {d},stats};
	}
	
	//Training
	public void train(LinkedList<Image> data, int batchSize, double learningRate) {
		System.out.println("Data Size: "+data.size());
		double seconds = getBackPropTime(data.getFirst())/1000.0;
		System.out.println("Estimated Completion: "+getETA(seconds*batchSize, (int)Math.ceil((data.size()-trainedOn)/batchSize)));
		System.out.println("Estimated Total Time: "+(int)(data.size()*seconds)+"s");
		long start = System.currentTimeMillis();
		Image image;
		for(int i = trainedOn; i < data.size();) { //Iterates through each batch
			long batchStart = System.currentTimeMillis();
			int correct = 0;
			image = data.get(i);
			backPropigate(image.getInput(), image.getOutput());
			double[][][] weightVector = dcdw;
			double[][] biasVector = dcdb;
			int batch = 1;
			i++;
			if(getGuess()==image.label) correct++;
			for(;i%batchSize != 0 && i < data.size();i++) { //Iterates through each data set in the batch
				image = data.get(i);
				backPropigate(data.get(i).getInput(), data.get(i).getOutput());
				//				printDiagnostic(image);
				weightVector = sumMatrix(weightVector, dcdw);
				biasVector = sumMatrix(biasVector, dcdb);
				batch++;
				if(getGuess()==image.label) correct++;
			}
			if(batch > 0) {
				divideMatrix(weightVector, batch);
				divideMatrix(biasVector, batch);
				//Make changes from batch
				multiplyMatrix(weightVector, learningRate*-1);
				multiplyMatrix(biases, learningRate*-1);
				weights = sumMatrix(weights, weightVector);
				biases = sumMatrix(biases, biasVector);	
//				System.out.printf("Batch #%d:\tAccuracy:%3.0f%%\t%2.2f%% Complete\t\tEst Complete: %s\n", i/batchSize, test(Image.getTestImageList())*100.0, (100.0*i)/data.size() ,getETA((System.currentTimeMillis()-batchStart)/1000.0, (int)Math.ceil((data.size()-i)/batchSize)));
				double[][] results = getStats(Image.getTestImageList());
				System.out.printf("Accuracy: %.3f%% ", results[0][0]*100);
				for(int ii = 0; ii < 10; ii++) {
					System.out.printf("%d:%.0f%% ",ii, results[1][ii]);
				}
				trainedOn += batch;
				save("Current.txt");
				if(results[0][0] > best) {
					best = results[0][0];
					save("Best.txt");
					System.out.println("New Best");
				}else {
					System.out.println();
				}
			}

		}
	}

	private int getBackPropTime(Image image) {
		long start = System.currentTimeMillis();
		backPropigate(image.getInput(), image.getOutput());
		return (int)(System.currentTimeMillis()-start);
	}

	private String getETA(double seconds, int batchesLeft) {
		SimpleDateFormat format = new SimpleDateFormat("hh:mm aa, EEEE");
		Calendar calender = GregorianCalendar.getInstance();
		int secondsLeft =  (int)(batchesLeft * seconds);
		calender.add(Calendar.SECOND, secondsLeft);
		return format.format(calender.getTime());
	}

	private double[][][] sumMatrix(double[][][] a, double[][][] b){
		if(a.length != b.length) {
			System.err.println("SUM MATRIX ERROR: Matracies not same shape");
			return null;
		}
		for(int i = 0; i < a.length; i++) {
			a[i] = sumMatrix(a[i], b[i]);
		}
		return a;
	}
	private void divideMatrix(double[][][] matrix, double c){
		for(int x = 0; x < matrix.length; x++)
			for(int y = 0; y < matrix[x].length; y++)
				for(int z = 0; z < matrix[x][y].length; z++)
					matrix[x][y][z] /= c;
	}
	private void divideMatrix(double[][] matrix, double c){
		for(int x = 0; x < matrix.length; x++)
			for(int y = 0; y < matrix[x].length; y++)
				matrix[x][y] /= c;
	}

	private double[][] sumMatrix(double[][] a, double[][] b){
		if(a.length != b.length) {
			System.err.println("SUM MATRIX ERROR: Matracies not same shape");
			return null;
		}
		for(int y = 0; y < a.length; y++) {
			if(a[y].length != b[y].length) {
				System.err.println("SUM MATRIX ERROR: Matracies not same shape");
				return null;
			}
			for(int x = 0; x < a[y].length; x++) {
				a[y][x] += b[y][x];
			}
		}
		return a;
	}

	private void multiplyMatrix(double[][] a, double c){
		for(int j = 0; j < a.length; j++) {
			for(int i = 0; i < a[j].length; i++)
				a[j][i] *= c;
		}
	}
	private void multiplyMatrix(double[][][] a, double c) {
		for(double[][] daa: a)
			multiplyMatrix(daa, c);
	}

	//Forward Propagation
	public int classify(Image image) {
		updateInput(image.getInput());
		propigateForward();
		return getGuess();
	}
	public void updateInput(double[] input) {
		if(input.length != structure[0]) {
			System.err.println("GET ACTIVATION ERROR: Input wrong shape");
			return;
		}
		this.input = input;
		propigateForward();
	}

	public double[] getOutput() {
		if(input == null) {
			System.err.println("GET ACTIVATION ERROR: No input");
			return new double[0];
		}
		return activations[activations.length-1];
	}
	public int getGuess() {
		double biggest = 0;
		int index = 0;
		for(int i = 0; i < structure[structure.length-1]; i++) {
			double val = activations[activations.length-1][i];
			if(val > biggest) {
				biggest = val;
				index = i;
			}
		}
		return index;
	}

	public double getCost(double[] label) {
		double cost = 0;
		for(int i = 0; i < label.length; i++) {
			cost += Math.pow(label[i]-getOutput()[i], 2);
		}
		return cost;
	}

	private void propigateForward() {
		activations[0] = sigmoid(sumProd(input, weights[0], biases[0]));
		for(int layer = 1; layer < structure.length - 1; layer++) {
			activations[layer] = sigmoid(sumProd(activations[layer-1], weights[layer], biases[layer]));
		}
	}

	private double[] sumProd(double[] prevActivtions, double[][] weights, double[] biases) {
		double[] rv = new double[weights.length];
		if(prevActivtions.length != weights[0].length) {
			System.err.println("SUMPROD ERROR: Matracies shapes don't match");
			return rv;
		}
		for(int j = 0; j < weights.length; j++) {
			rv[j] = biases[j];
			for(int k = 0; k < weights[0].length; k++) {
				rv[j] += prevActivtions[k]*weights[j][k];
			}
		}
		return rv;
	}
	//Back Propagation
	private void backPropigate(double[] data, double[] label) {
		updateInput(data);
		propigateForward();
		dcdw = emptyWArr(structure);
		dcdb = emptyBArr(structure);
		dcda = emptyBArr(structure);
		for(int layer = structure.length-2; layer >= 0; layer--) {
			for(int j = 0; j < structure[layer+1]; j++) {
				dcdb[layer][j] = getDcDb(layer, j, label);
				for(int k = 0; k < structure[layer]; k++) {
					dcdw[layer][j][k] = getDcDw(layer, j, k, label);
				}
			}
		}
	}

	private double getDcDw(int l, int j, int k, double[] label) {
		double dzdw = getActivation(l-1, k);
		double a = getActivation(l, j);
		double z = sigmoidInv(a);
		double dadz = sigmoidP(z);
		double dcda = getDcDa(l, j, label);
		return dzdw * dadz * dcda;
	}
	private double getDcDb(int l, int j, double[] label) {
		double a = getActivation(l, j);
		double z = sigmoidInv(a);
		double dadz = sigmoidP(z);
		double dcda = getDcDa(l, j, label);
		return dadz * dcda;
	}

	private double getDcDa(int l, int k, double[] label) {
		double dcdak = 0.0;
		if(l == structure.length - 2) {
			dcdak = 2 * (getOutput()[k]-label[k]);
		}else if(l == structure.length - 1) {
			return 1;
		}else {
			for(int j = 0; j < structure[l+2]; j++) {
				double dzdak = weights[l+1][j][k]; //<-- Error point
				double a = getActivation(l+1, j);
				double z = sigmoidInv(a);
				double dadz = sigmoidP(z);
				//				double dcda = getDcDa(l+1, j, label);
				double dcda = this.dcda[l+1][j];
				dcdak += dzdak * dadz * dcda;
			}
		}
		dcda[l][k] = dcdak;
		return dcdak;
	}

	private double getActivation(int l, int j) {
		if(l >= structure.length || l < -1) {
			System.err.println("GET ACTIVATION ERROR: Layer index out of bounds");
			return 0.0;
		}
		if(j >= structure[l+1] || j < 0) {
			System.err.println("GET ACTIVATION ERROR: Node index out of bounds");
			return 0.0;
		}
		if(input == null) {
			System.err.println("GET ACTIVATION ERROR: No input");
			return 0.0;
		}
		if(input.length != structure[0]) {
			System.err.println("GET ACTIVATION ERROR: Input wrong shape");
			return 0.0;
		}
		if(l == -1)
			return input[j];
		return activations[l][j];
	}

	//Helper Methods
	private double sigmoid(double x) {
		return 1/(1+Math.exp(-x));
	}
	private double[] sigmoid(double[] x) {
		double[] rv = new double[x.length];
		for(int i = 0; i < rv.length; i++) {
			rv[i] = sigmoid(x[i]);
		}
		return rv;
	}
	private double sigmoidP(double x) {
		double sig = sigmoid(x);
		return sig*(1-sig);
	}
	private double sigmoidInv(double x) {
		return Math.log(x/(1-x));
	}

	private void print() {
		//		print(dcdw);
		//		printGraph();
		//		printWeights();
		printBiases();
	}

	private void printGraph() {
		System.out.print("In\t");
		for(int i = 1; i < structure.length; i++) {
			System.out.print("L"+i+"\t");
		}
		System.out.println();
		boolean letter = true;
		char c = 'A';
		int maxNodes = 0;
		for(int l = 0; l < structure.length; l++)
			maxNodes = Math.max(maxNodes, structure[l]);
		char[][] graph = new char[maxNodes][structure.length];
		for(int l = 0; l < structure.length; l++) {
			int start = (maxNodes-structure[l])/2;
			for(int i = 0; i < structure[l]; i++) {
				graph[i+start][l] = c++;
			}
			if(letter) c = '1';
			else c = 'A';
			letter = !letter;
		}
		for(int y = 0; y < graph.length; y++) {
			for(int x = 0; x < graph[y].length; x++) {
				System.out.print(graph[y][x]+"\t");
			}
			System.out.println();
		}
	}

	private void printWeights() {
		for(int l = 0; l < structure.length-1; l++) {
			System.out.println("Layer "+l);
			for(int j = 0; j < structure[l+1]; j++) {
				for(int k = 0; k < structure[l]; k++) {
					char jc = (char)('A' + j);
					char kc = (char)('0' + k);
					if(l%2==0) {
						jc = (char)('0'+k);
						kc = (char)('A'+j);
					}
					System.out.printf("%c%c:\t%5.2f\n",kc,jc,weights[l][j][k]);
				}
				System.out.println();
			}
		}
	}

	private void printBiases() {
		for(int l = 0; l < structure.length-1; l++) {
			System.out.println("Layer "+l);
			for(int k = 0; k < structure[l+1]; k++) {
				char c = (char)('0' + k);
				if(l%2==0) {
					c = (char)('A'+k);
				}
				System.out.printf("%c:\t%5.2f\n",c,biases[l][k]);
			}
			System.out.println();
		}
	}

	public static void print(double[][][] a) {
		for(double[][] daa : a) {
			print(daa);
		}
	}
	public static void print(double[][] a) {
		System.out.print("[");
		for (int i = 0; i < a.length; i++) {
			if (i != 0)
				System.out.print("\n [");
			else
				System.out.print("[");
			for (int j = 0; j < a[i].length - 1; j++) {
				System.out.printf("%3.4f, ",a[i][j]);
			}
			System.out.printf("%3.4f]",a[i][a[i].length - 1]);
		}
		System.out.println("]");
	}

	private void printDiagnostic(Image image) {
		System.out.printf("%s\tCost: %.3f\tLabel: %d\t Guess: %d\n",(image.label == getGuess() ? "CORRECT" : "WRONG"),getCost(image.getOutput()), image.label, getGuess());
	}

	//File
	public void load(String file) {
		try {
			Scanner in = new Scanner(new File(file));	
			trainedOn = Integer.parseInt(in.nextLine());
			structure = sti(in.nextLine().split(" "));
			weights = new double[structure.length-1][][];
			activations = new double[structure.length-1][];
			for(int l = 0; l < structure.length-1; l++) {
				weights[l] = new double[structure[l+1]][];
				activations[l] = new double[structure[l+1]];
				for(int j = 0; j < structure[l+1]; j++) {
					weights[l][j] = std(in.nextLine().split(" "));
				}
			}
			biases = new double[structure.length-1][];
			for(int l = 0; l < structure.length-1; l++)
				biases[l] = std(in.nextLine().split(" "));
			in.close();
		} catch (FileNotFoundException e) {
			System.err.println("Saved File Not Found");
		}

	}
	public static int[] sti(String[] args) {
		int[] ret = new int[args.length];
		for (int i = 0; i < ret.length; i++)
			ret[i] = Integer.parseInt(args[i]);
		return ret;
	}
	public static double[] std(String[] args) {
		double[] ret = new double[args.length];
		for (int i = 0; i < ret.length; i++)
			ret[i] = Double.parseDouble(args[i]);
		return ret;
	}

	public void save(String file) {
		PrintWriter print;
		try {
			print = new PrintWriter(file);
			print.println(trainedOn);
			for(int i:structure) print.print(i+ " ");
			print.println();
			for(int x = 0; x < weights.length; x++)
				for(int y = 0; y < weights[x].length; y++) {
					for(int z = 0; z < weights[x][y].length; z++)
						print.print(weights[x][y][z]+" ");
					print.println();
				}
			for(int x = 0; x < biases.length; x++) {
				for(int y = 0; y < biases[x].length; y++)
					print.print(biases[x][y]+" ");
				print.println();
			}
			print.flush();
		} catch (FileNotFoundException e) {
			System.err.println("Could not save");
		}
	}

	public static void main(String[] args) {
//		infiniteTrain();
		singleCompleteTrain();
	}
	public static void singleCompleteTrain() {
		LinkedList<Image> images = Image.getTrainingImageList();
		NN1 neuralNetwork = new NN1();
		neuralNetwork.trainedOn = 0;
		neuralNetwork.train(images, 100, .2);
	}
	public static void infiniteTrain() {
		LinkedList<Image> images = Image.getTrainingImageList();
		NN1 neuralNetwork = new NN1();//new int[] {784,36,16,10} new int[] {784,100,60,40,10}
		while(true) {
			if(neuralNetwork.trainedOn == images.size()) neuralNetwork.trainedOn = 0;
			neuralNetwork.train(images, 100, .2);
		}
	}
	public static void beep() {
		try {
			new ProcessBuilder("afplay", "/System/Library/Sounds/Basso.aiff").start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
