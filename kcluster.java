import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.lang.Math;


public class kcluster
{
	// Using CSR(Compressed sparse row) to store sparse matrix
	private ArrayList<Integer> _newid; //_newid[i] is the id of the i row
	private ArrayList<Integer> _row_offsets; //_row_offsets[i] is the offset of the i row
	private ArrayList<Integer> _columns; //_columns[_row_offsets[i]] to _columns[_row_offsets[i+1]-1] is the dimensions of i row
	private ArrayList<Double> _values; //_values[_row_offsets[i]] is the value of (i, _columns[_row_offsets[i]])

	private ArrayList<Integer> _kclass;  // _kclass[i] is the label of ith data
	private Map<Integer, String> _class; // Actual class for each data: <ID, class>

	// centroids: the center of clusters
	private ArrayList<Integer> _cen_row_offsets;
	private ArrayList<Integer> _cen_cols;
	private ArrayList<Double> _cen_values;

	// tmp_centroids
	private ArrayList<Integer> _tmpcen_row_offsets;
	private ArrayList<Integer> _tmpcen_cols;
	private ArrayList<Double> _tmpcen_values;

	private int _row_num, _dim_num; // the number of rows and dimensions

	private int _clusters_num; // the number of clusters;

	private static int [] seeds = {1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35,37,39};


	public kcluster(String input_file, String class_file) throws IOException{
		BufferedReader br;
		String line;
		String[] ls;

		//Reading data
		System.out.println("Reading data from: "+input_file+"...");
		br = new BufferedReader(new FileReader(input_file));
		_newid = new ArrayList<Integer>();
		_row_offsets = new ArrayList<Integer>();
		_columns = new ArrayList<Integer>();
		_values = new ArrayList<Double>();

		int offset = 0;
		int newid = 0;
		_dim_num = 0;
		_row_num = -1;

		while ((line = br.readLine())!=null){
			ls = line.split(",");
			if (newid != Integer.parseInt(ls[0])) {
				newid = Integer.parseInt(ls[0]);
				_newid.add(newid);
				_row_offsets.add(offset);
				_row_num++;
			}
			
			_columns.add(Integer.parseInt(ls[1]));
			_values.add(Double.parseDouble(ls[2]));
			offset++;

			if (Integer.parseInt(ls[1]) > _dim_num) {
				_dim_num = Integer.parseInt(ls[1]);
			}
		}
		//Add the total number of non-zero entries in the matrix to the end of _row_offsets
		_row_offsets.add(_columns.size());

		br.close();
		//_row_num = _newid.size();
		System.out.println("Reading data finished...");
		System.out.println("Row number: "+_row_num);
		System.out.println("Dimension number: "+_dim_num);
		System.out.println("_newid size: "+_newid.size());
		System.out.println("_row_offsets size: "+_row_offsets.size());
		System.out.println("_columns size: "+_columns.size());
		System.out.println("_values size: "+_values.size());
		System.out.println();
		

		//Reading class file
		System.out.println("Reading data from: "+class_file+"...");
		br = new BufferedReader(new FileReader(class_file));
		_class = new LinkedHashMap<Integer, String>();

		while ((line = br.readLine())!=null){
			ls = line.split(",");
			_class.put(Integer.parseInt(ls[0]), ls[1]);
		}

		br.close();
		System.out.println("Reading data finished...");
		System.out.println("_class size: "+_class.size());
		System.out.println();
	}

	public void clustering(int clusters_num, int trials_num, int max_iter, String criterion_function, double threshold) {
		_clusters_num = clusters_num;

		for (int i = 0; i < trials_num; i++) {
			System.out.println("Processing "+(i+1)+"th trial...");

			//Randomly initailize centroids
			_cen_row_offsets = new ArrayList<Integer>();
			_cen_cols = new ArrayList<Integer>();
			_cen_values = new ArrayList<Double>();

			Random rand = new Random(seeds[i]);

			int cen_offset = 0;
			for (int j = 0; j < clusters_num; j++) {
				int r = rand.nextInt(_row_num);
				_cen_row_offsets.add(cen_offset);

				int offset = _row_offsets.get(r);
				int length = _row_offsets.get(r+1)-offset;
				for (int k=0; k<length; k++) {
					_cen_cols.add(_columns.get(offset+k));
					_cen_values.add(_values.get(offset+k));
					cen_offset++;
				}
			}
			_cen_row_offsets.add(_cen_cols.size());


			int count = 0;
			while (true) {
				System.out.println("Processing the "+(count+1)+"th iteration..");

				_kclass = new ArrayList<Integer>();
				for (int j = 0; j < _row_num; j++) {
					//System.out.println("Process the "+(j+1)+"th data..");
					int label = assignData(j);
					_kclass.add(label);
				}

				computeTempCentroids();

				_cen_row_offsets = new ArrayList<Integer>();
				_cen_cols = new ArrayList<Integer>();
				_cen_values = new ArrayList<Double>();

				/*
				for(int v : _tmpcen_row_offsets) {
					_cen_row_offsets.add(v);
				}
				for(int v : _tmpcen_cols) {
					_cen_cols.add(v);
				}
				for(double v : _tmpcen_values) {
					_cen_values.add(v);
				}
				*/

				System.out.println("_cen_row_offsets size: "+_cen_row_offsets.size());
				System.out.println("_cen_cols size: "+_cen_cols.size());
				System.out.println("_cen_values size: "+_cen_values.size());

				count++;
				if (count > max_iter || converge(threshold)) {
					break;
				}
			}
			
		}
	}

	// compute the distance
	// mode = 0: distance between data and centroid
	// mode = 1: distance between new centroid and old centroid
	private double distance(int a, int b, int mode) {
		int length;
		int offset;

		offset = _row_offsets.get(a);
		length = _row_offsets.get(a+1)-offset;
		ArrayList<Integer> a_columns = new ArrayList<Integer>();
		ArrayList<Double> a_values = new ArrayList<Double>();
		for (int i=0; i<length; i++) {
			a_columns.add(_columns.get(offset+i));
			a_values.add(_values.get(offset+i));
			//System.out.println(_values.get(offset));
		}

		offset = _cen_row_offsets.get(b);
		length = _cen_row_offsets.get(b+1)-offset;
		ArrayList<Integer> b_columns = new ArrayList<Integer>();
		ArrayList<Double>  b_values = new ArrayList<Double>();
		for (int i=0; i<length; i++) {
			b_columns.add(_cen_cols.get(offset+i));
			b_values.add(_cen_values.get(offset+i));
		}

		int max_d = Math.max(Collections.max(a_columns), Collections.max(b_columns));
		//System.out.println("max dimension: "+max_d);
		
		ArrayList<Integer> share_dimension = new ArrayList<Integer>();
		double d = 0;
		double result = 0;
		for (int i=0; i < a_columns.size(); i++) {
			int dim = a_columns.get(i);
			if (b_columns.contains(dim)) {
				d = a_values.get(i)-b_values.get(b_columns.indexOf(dim));
				share_dimension.add(dim);
			}
			else {
				d = a_values.get(i);
			}
			result += d*d;
		}

		for (int i=0; i < b_columns.size(); i++) {
			int dim = b_columns.get(i);
			if (a_columns.contains(dim)) {
				if (!share_dimension.contains(dim)) {
					d = b_values.get(i)-a_values.get(a_columns.indexOf(dim));
				}
			}
			else {
				d = b_values.get(i);
			}
			result += d*d;
		}
		return Math.sqrt(result);
	}

	// assign the data to closest centroid
	private int assignData(int a) {
		double min_distance = distance(a, 0, 0);
		int label = 0;
		for (int i = 0; i < _clusters_num; i++) {
			double d = distance(a, i, 0);
			// /System.out.println(d);
			if (d < min_distance) {
				min_distance = d;
				label = i;
			}
		}
		return label;
	}

	private void computeTempCentroids() {
		ArrayList<Integer> labelCount = new ArrayList<Integer>();
		ArrayList<Integer> _tmpcen_rows = new ArrayList<Integer>();
		_tmpcen_row_offsets = new ArrayList<Integer>();
		_tmpcen_cols = new ArrayList<Integer>();
		_tmpcen_values = new ArrayList<Double>();

		for (int i=0; i<_clusters_num; i++) {
			labelCount.add(0);
		}

		for (int i=0; i<_row_num; i++) {
			int label = _kclass.get(i);

			int offset = _row_offsets.get(i);
			int length = _row_offsets.get(i+1)-offset;
			for (int j=0; j < length; j++) {
				int dim = _columns.get(offset+j);
				if (_tmpcen_rows.contains(label)) {
					int row_index = _tmpcen_rows.lastIndexOf(label);
					if (!_tmpcen_cols.contains(dim)) {
						_tmpcen_rows.add(row_index+1, label);
						_tmpcen_cols.add(row_index+1, _columns.get(offset+j));
						_tmpcen_values.add(row_index+1, _values.get(offset+j));
					}
					else {
						int dim_index = _tmpcen_cols.indexOf(dim);
						double v = _tmpcen_values.get(dim_index)+_values.get(offset+j);
						_tmpcen_values.set(dim_index, v);
					}
				}
				else {
					_tmpcen_rows.add(label);
					_tmpcen_cols.add(_columns.get(offset+j));
					_tmpcen_values.add(_values.get(offset+j));
				}
			}
			labelCount.set(label, labelCount.get(label)+1);
		}

		for (int i=0; i<_tmpcen_rows.size(); i++) {
			int count = labelCount.get(_tmpcen_rows.get(i));
			_tmpcen_values.set(i, _tmpcen_values.get(i)/count);
		}

		int offset = 0;
		int row = -1;
		for (int i=0; i<_tmpcen_rows.size(); i++) {
			int temp = _tmpcen_rows.get(i);
			if (row != temp) {
				_tmpcen_row_offsets.add(offset);
				row = temp;
			}
			offset++;
		}
		_tmpcen_row_offsets.add(_tmpcen_cols.size());
		
	}

	private boolean converge(double threshold) {
		/*
		if ()
			return true;
		else
			return false;
		*/
		return false;
	}


	public static void main(String[] args) throws IOException{
		String input_file = args[0];
		String criterion_function = args[1];
		String class_file = args[2];
		String clusters_num = args[3];
		String trials_num = args[4];
		String output_file = args[5];

		System.out.println("input file: "+input_file);
		System.out.println("criterion function: "+criterion_function);
		System.out.println("class file: "+class_file);
		System.out.println("No. of clusters:"+clusters_num);
		System.out.println("No. of trials:"+trials_num);
		System.out.println("output file: "+output_file);
		System.out.println();

		kcluster k = new kcluster(input_file, class_file);

		k.clustering(Integer.parseInt(clusters_num), 1, 10, criterion_function, 0.001);
		
	}
}