package edu.wisc.cs.arc.modifiers;

import java.util.*;

import gurobi.*;
import org.jgrapht.GraphPath;

import edu.wisc.cs.arc.graphs.DirectedEdge;
import edu.wisc.cs.arc.graphs.ExtendedTopologyGraph;
import edu.wisc.cs.arc.graphs.Vertex;

public class CanonicalETGConverterGurobi<V extends Vertex> 
		extends CanonicalETGConverter<V> {

	protected GRBModel _model;
	protected Map<String, GRBVar> _variables;

	public CanonicalETGConverterGurobi(ExtendedTopologyGraph<V> etg)
			throws ModifierException {
		// by default we will use 10 paths, thus resulting in 9 constraints
		this(etg, 10);
	}

	public CanonicalETGConverterGurobi(ExtendedTopologyGraph<V> etg, 
			int num_paths) throws ModifierException {
		super(etg, num_paths);
		initGurobi();
	}

	private void initGurobi() throws ModifierException {
		try {
			_model = new GRBModel(new GRBEnv());
			_model.getEnv().set(GRB.IntParam.LogToConsole, 0);
			_variables = new HashMap<>();
		} catch(GRBException e) {
			throw new ModifierException(e.getMessage());
		}
	}

	private void setupVariables() throws GRBException {
		// 1. We create a non-negative variable for each edge in the ETG
		int i = 0;
		for (DirectedEdge<V> edge : _etg.getGraph().edgeSet()) {
			try {
				GRBVar tmp = _model.addVar(0.0, UPPER_BOUND, 0.0,
						GRB.CONTINUOUS, edge.getName());
				_variables.put(edge.getName(), tmp);
				i++;
			} catch (GRBException e) {
				throw e;
			}
		}
		_model.update();
		_logger.debug("Size (_variables) = " + _variables.size());
		_logger.debug("i = " + i);
		_logger.debug("Num variables in model = " 
				+ _model.get(GRB.IntAttr.NumVars));
	}

	private void setupNonNegativityConstraints() throws GRBException {
		for(String name : _variables.keySet()) {
			GRBVar w = _variables.get(name);
			_model.addConstr(w, GRB.GREATER_EQUAL, 0.0, "nn-"+name);
		}
		_model.update();
	}

	private void setupConstraints(List<GraphPath<V, DirectedEdge<V>>> paths)
			throws GRBException {
		// For each path the sum of edge weights should be
		// less than the sum of edge weights in the next longest path
		for (int i = 0; i < paths.size() - 1; i++) {
			GraphPath<V, DirectedEdge<V>> path_one = paths.get(i);
			GraphPath<V, DirectedEdge<V>> path_two = paths.get(i + 1);

			Double rhs = _PATH_GAP;
			if(Math.abs(getPathLength(path_one) - getPathLength(path_two)) 
					< EPSILON) {
				rhs = 0.0;
			}

			GRBLinExpr expr = new GRBLinExpr();
			for (DirectedEdge<V> edge : path_two.getEdgeList()) {
				GRBVar w = _variables.get(edge.getName());
				expr.addTerm(1.0, w);
			}

			for (DirectedEdge<V> edge : path_one.getEdgeList()) {
				GRBVar w = _variables.get(edge.getName());
				expr.addTerm(-1.0, w);
			}

			_model.addConstr(expr, GRB.GREATER_EQUAL, rhs, "eq-" + i);
		}
		_model.update();
		_logger.debug("Number of constraints = " 
				+ _model.get(GRB.IntAttr.NumConstrs));
		_logger.debug("Expected number of constraints = " 
				+ (_ordered_edges.size() + paths.size()-1));
	}

	private void setupObjective() throws GRBException {
		int i = 0;
		try {
			GRBLinExpr expr = new GRBLinExpr();
			for (String name : _ordered_edges) {
				GRBVar w = _variables.get(name);
				expr.addTerm(1 + _LAMBDA * i, w);
			}
			_model.setObjective(expr, GRB.MINIMIZE);
			_model.update();
		} catch (GRBException e) {
			_logger.debug("Error in setting up the objective.");
			throw e;
		}
	}

	private void solve() throws GRBException {
		try {
			_model.update();
			_model.optimize();
		} catch (GRBException e) {
			System.out.println("Error in optimizing the model");
			throw e;
		}
	}

	private void checkSolution() throws GRBException {
		if(_model.get(GRB.IntAttr.Status) == GRB.OPTIMAL) {
			_logger.debug("The solution obtained is optimal.");
		} else {
			_logger.debug("The solution obtained is NOT optimal." +
					" Status: " + _model.get(GRB.IntAttr.Status));
		}
	}


	protected Map<DirectedEdge<V>, Double> obtainEdgeWeights(
			List<GraphPath<V, DirectedEdge<V>>> paths) {
		Map<DirectedEdge<V>, Double> retval = new HashMap<>();
		try {
			setupVariables();
			setupNonNegativityConstraints();
			setupConstraints(paths);
			setupObjective();
			solve();
			checkSolution();
		} catch (GRBException e) {
			throw new ModifierException(e.getMessage());
		}
		// retrieve the solution
		for (DirectedEdge<V> edge : _etg.getGraph().edgeSet()) {
			try{
				GRBVar w = _variables.get(edge.getName());
				retval.put(edge, w.get(GRB.DoubleAttr.X));
			} catch (GRBException e) {
				_logger.debug("Error in retrieving the solution. Edge: " 
						+ edge.getName());
				throw new ModifierException(e.getMessage());
			}
		}
		return retval;
	}
}
