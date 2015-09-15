package pppp.g5;

// import java.util.Random;

import pppp.sim.Move;
import pppp.sim.Point;


public class Player implements pppp.sim.Player {

	// see details below
	private int id = -1;
	private int side = 0;
	private int[] pos_index = null;
	private Point[][] pos = null;
	private double[] range  = null;
	// private Point[] random_pos = null;
	private Point[] last_rat_pos = null;
	private boolean[] catched_rat = null;
	private boolean playing = false;
	// private Point current_target = null;
	// private Random gen = new Random();

	// create move towards specified destination
	private static Move move(Point src, Point dst, boolean play)
	{
		double dx = dst.x - src.x;
		double dy = dst.y - src.y;
		double length = Math.sqrt(dx * dx + dy * dy);
		double limit = play ? 0.1 : 0.5;
		if (length > limit) {
			dx = (dx * limit) / length;
			dy = (dy * limit) / length;
		}
		return new Move(dx, dy, play);
	}
	
	// generate point after negating or swapping coordinates
	private static Point point(double x, double y,
	                           boolean neg_y, boolean swap_xy)
	{
		if (neg_y) y = -y;
		return swap_xy ? new Point(y, x) : new Point(x, y);
	}

	private boolean in_square(double x, double y){
		if (x>-side*0.5 && x<side*0.5 && y>-side*0.5 && y<side*0.5)
			return true;
		else 
			return false;
	}
	// find the nearest meet position between pipers and rats
	private Point find_nearest_rat(Point piper, Point[] rats)
	{
		int target_rat_ID = -1;
		double time = Double.MAX_VALUE;
		double x = 0;
		double y = 0;
		Point ans = new Point(x,y);
		for(int i=0;i< rats.length;i++){
			if (catched_rat[i])
				continue;
			double dist_x = rats[i].x-piper.x;
			double dist_y = rats[i].y-piper.y;
			double speed_x = rats[i].x-last_rat_pos[i].x;
			double speed_y = rats[i].y-last_rat_pos[i].y;
			double dist = Math.sqrt(dist_x*dist_x+dist_y*dist_y);
			double speed_vertical = speed_y*dist_y/dist+speed_x*dist_x/dist;
			double speed_parallel = speed_x*dist_y/dist-speed_y*dist_x/dist;
			double speed_piper_vertical = Math.sqrt(0.5*0.5-speed_parallel*speed_parallel);
			double time_est = (dist-8)/(speed_piper_vertical-speed_vertical);
			if (0< time_est && time_est < time){
				double dst_x = piper.x + time_est*(speed_piper_vertical*dist_x/dist+speed_parallel*dist_y/dist);// rats[i].x + time_est*speed_x;//speed_parallel*time_est* dist_y / dist;
				double dst_y = piper.y + time_est*(speed_piper_vertical*dist_y/dist-speed_parallel*dist_x/dist);// rats[i].y + time_est*speed_y;//speed_parallel*time_est* dist_x / dist;
				if (in_square(dst_x,dst_y)){
					time = time_est;
					ans = new Point(dst_x,dst_y);
					target_rat_ID = i;
				}
			}
		}
		if (target_rat_ID!=-1)
			catched_rat[target_rat_ID] = true;
		return ans;
	}

	// find the next rat while playing music
	private Point find_next_rat(Point piper, Point[] rats,double max_time)
	{
		int target_rat_ID = -1;
		Point ans = null;
		for(int i=0;i< rats.length;i++){
			if (catched_rat[i])
				continue;
			double dist_x = rats[i].x-piper.x;
			double dist_y = rats[i].y-piper.y;
			double speed_x = rats[i].x-last_rat_pos[i].x;
			double speed_y = rats[i].y-last_rat_pos[i].y;
			double dist = Math.sqrt(dist_x*dist_x+dist_y*dist_y);
			if (dist<10){
				catched_rat[i] = true;
				continue;
			}
			double speed_vertical = speed_y*dist_y/dist+speed_x*dist_x/dist;
			double speed_parallel = speed_x*dist_y/dist-speed_y*dist_x/dist;
			double speed_piper_vertical = Math.sqrt(0.1*0.1-speed_parallel*speed_parallel);
			double time_est = (dist-8)/(speed_piper_vertical-speed_vertical);
			if (0< time_est && time_est < max_time){
				double dst_x =piper.x + time_est*(speed_piper_vertical*dist_x/dist+speed_parallel*dist_y/dist);//rats[i].x + time_est*speed_x;//speed_parallel*time_est* dist_y / dist;
				double dst_y =piper.y + time_est*(speed_piper_vertical*dist_y/dist-speed_parallel*dist_x/dist);//rats[i].y + time_est*speed_y;//speed_parallel*time_est* dist_x / dist;
				if (in_square(dst_x,dst_y)){

					max_time = time_est;
					ans = new Point(dst_x,dst_y);
					target_rat_ID = i;
				}
			}
		}
		if (target_rat_ID!=-1)
			catched_rat[target_rat_ID] = true;
		return ans;
	}
	
	// specify location that the player will alternate between
	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats)
	{
		this.id = id;
		this.side = side;
		int n_pipers = pipers[id].length;
		// random_pos = new Point [n_pipers];
		pos_index = new int [n_pipers];
		pos = new Point [n_pipers][3];
		range = new double[3];
		range[2] = range[0] = 0.000001;
		range[1] = 0.000001;
		for (int p = 0 ; p != n_pipers ; ++p) {
			// spread out at the door level
			// double door = 0.0;
			// if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
			// pick coordinate based on where the player is
			boolean neg_y = id == 2 || id == 3;
			boolean swap  = id == 1 || id == 3;
			// first and third position is at the door
			pos[p][0] = point(0, side * 0.5, neg_y, swap);
			// second position is chosen randomly in the rat moving area
			//pos[p][1] = null;
			//pos[p][2] = null;
			// fourth and fifth positions are outside the rat moving area
			// pos[p][3] = point(door * -6, side * 0.5 + 3, neg_y, swap);
			// pos[p][4] = point(door * +6, side * 0.5 + 3, neg_y, swap);
			// start with first position
			pos_index[p] = 0;
		}
		last_rat_pos = new Point[rats.length];
		for (int i = 0 ; i != rats.length ; ++i) {
			last_rat_pos[i] = new Point(rats[i].x,rats[i].y);
		}
	}

	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			Point src = pipers[id][p];
			Point dst = pos[p][pos_index[p]];
			// if (pos_index[p]>0) dst = current_target;
			// if position is reached
			if (Math.sqrt((src.x - dst.x)*(src.x - dst.x)+(src.y - dst.y)*(src.y - dst.y)) < range[pos_index[p]])  {
				System.out.println("rats_NO:"+rats.length);
				if (++pos_index[p] == pos[p].length) pos_index[p] = 0;
				if (pos_index[p] == 0){
					playing = false;
				}
				else if (pos_index[p] == 1){					
					catched_rat = new boolean[rats.length];
					for(int i=0;i<rats.length;i++) {catched_rat[i] = false;}
					pos[p][pos_index[p]] = find_nearest_rat(pipers[id][p],rats);
				}
				else if (pos_index[p] == 2){
					playing =true;
					double max_time = 200;  
					//double max_time = Math.sqrt((pipers[id][p].x-pos[p][0].x)*(pipers[id][p].x-pos[p][0].x)
					//		 +(pipers[id][p].y-pos[p][0].y)*(pipers[id][p].y-pos[p][0].y))/0.1; 
					pos[p][pos_index[p]] = find_next_rat(pipers[id][p],rats,max_time);
					if (pos[p][pos_index[p]] ==null){
						double dist_x = (pos[p][0].x-pos[p][1].x);
						double dist_y = (pos[p][0].y-pos[p][1].y);
						double dist = Math.sqrt(dist_x*dist_x+dist_y*dist_y);
						pos[p][pos_index[p]] = new Point(pos[p][0].x+8*dist_x/dist,pos[p][0].y+8*dist_y/dist);
					}
					else{
						pos_index[p]--;
						pos[p][pos_index[p]] = new Point(pos[p][pos_index[p]+1].x,pos[p][pos_index[p]+1].y);
						double dist_x = (pos[p][0].x-pos[p][2].x);
						double dist_y = (pos[p][0].y-pos[p][2].y);
						double dist = Math.sqrt(dist_x*dist_x+dist_y*dist_y);
						pos[p][pos_index[p]+1] = new Point(pos[p][0].x+4*dist_x/dist,pos[p][0].y+4*dist_y/dist);
					}
					if (pos_index[p] == pos[p].length) {
						pos_index[p] = 0;
					}
				}
				dst = pos[p][pos_index[p]];
			}
			// get move towards position
			moves[p] = move(src, dst, playing);
		}

		last_rat_pos = new Point[rats.length];
		for (int i = 0 ; i != rats.length ; ++i) {
			last_rat_pos[i] = new Point(rats[i].x,rats[i].y);
		}
	}
}
