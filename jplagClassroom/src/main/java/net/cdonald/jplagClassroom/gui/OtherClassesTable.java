package net.cdonald.jplagClassroom.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import net.cdonald.jplagClassroom.googleCommunication.ClassroomInfo;
import net.cdonald.jplagClassroom.mainProgramData.MainClassroomData;
import net.cdonald.jplagClassroom.mainProgramData.MainClassroomDataListener;

@SuppressWarnings("serial")
public class OtherClassesTable extends JTable implements MainClassroomDataListener {
	private static final String [] COLUMN_NAMES= {"Year", "Class", "Assignment"};
	private static final int MIN_ROWS = 15;
	private JComboBox<String> yearCombo;
	private MainClassroomData classroomData;
	public class SelectedAssignment {
		private ClassroomInfo course;
		private ClassroomInfo assignment;
		public SelectedAssignment(ClassroomInfo course, ClassroomInfo assignment) {
			super();
			this.course = course;
			this.assignment = assignment;
		}
		public ClassroomInfo getCourse() {
			return course;
		}
		public ClassroomInfo getAssignment() {
			return assignment;
		}

	}
	public OtherClassesTable(MainClassroomData data, JProgressBar progress) {
		super();
		
		classroomData = data;
		classroomData.addListener(this);
		yearCombo = new JComboBox<String>();
		DefaultCellEditor yearEditor = new DefaultCellEditor(yearCombo);
		this.setModel(new DefaultTableModel(COLUMN_NAMES, MIN_ROWS));
		getColumn(COLUMN_NAMES[0]).setCellEditor(yearEditor);
		getColumn(COLUMN_NAMES[1]).setCellEditor(new ClassColumnEditor(data, progress));
		getColumn(COLUMN_NAMES[2]).setCellEditor(new AssignmentColumnEditor(data, progress));
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(getModel());
		sorter.setSortable(0, false);
		
	}
	
	
	@Override
	public void initComplete() {
		classroomData.fillYearComboBox(yearCombo);
		getTableHeader().setBackground(Color.GRAY);
		getTableHeader().setForeground(Color.BLUE);
		
	}
	
	public List<SelectedAssignment> getSelectedAssignments() {
		List<SelectedAssignment> assignments = new ArrayList<SelectedAssignment>();
		TableModel model = this.getModel();
		for (int i = 0; i < MIN_ROWS; i++) {
			ClassroomInfo course = (ClassroomInfo)model.getValueAt(i, 1);
			if (course != null) {
				ClassroomInfo assignment = (ClassroomInfo)model.getValueAt(i, 2);
				if (assignment != null) {
					assignments.add(new SelectedAssignment(course, assignment));
				}
			}
		}
		return assignments;
	}
	

	
	
	abstract class ClassInfoColumnEditor extends AbstractCellEditor implements TableCellEditor {
		private JComboBox<ClassroomInfo> currentCombo = null;
		private JComboBox<ClassroomInfo> emptyCombo;
		protected JProgressBar  progressBar;		
		public ClassInfoColumnEditor(JProgressBar  progressBar) {
			this.progressBar = progressBar;
			emptyCombo = new JComboBox<ClassroomInfo>();
			currentCombo = emptyCombo; 
		}
		
		@Override
		public Object getCellEditorValue() {
			if (currentCombo != null) {
				return currentCombo.getSelectedItem();
			}
			return null;
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

			currentCombo = getCurrentCombo(table, row, column);
			if (currentCombo != null) {
				currentCombo.setSelectedItem(value);
			}
			else {
				currentCombo = emptyCombo;
			}

			
			return currentCombo;
		}
		
		protected abstract JComboBox<ClassroomInfo> getCurrentCombo(JTable table, int row, int column);
		protected ItemListener getItemListener() {
			ItemListener itemListener = new ItemListener() {
				public void itemStateChanged(ItemEvent itemEvent) {
					if (stopCellEditing()) {
						fireEditingStopped();
					}
				}
			};
			return itemListener;
		}
	}
	
	class ClassColumnEditor extends ClassInfoColumnEditor {		
		private MainClassroomData classroomData;		
		private Map<String, JComboBox<ClassroomInfo>> possibleClasses;
		


		public ClassColumnEditor(MainClassroomData data, JProgressBar progressBar) {
			super(progressBar);
			classroomData = data;			
			possibleClasses = new HashMap<String, JComboBox<ClassroomInfo>>();
			
		}

		@Override
		public JComboBox<ClassroomInfo> getCurrentCombo(JTable table, int row, int column) {
			String selectedYear = (String)table.getModel().getValueAt(row, 0);
			if (selectedYear != null && selectedYear.length() > 0) {
				if (possibleClasses.containsKey(selectedYear) == false) {
					JComboBox<ClassroomInfo> classCombo = new JComboBox<ClassroomInfo>();
					classCombo.addActionListener((l)-> {
						ClassroomInfo info = (ClassroomInfo)classCombo.getSelectedItem();
						if (info != null) {
							classroomData.fillAssignments(info, progressBar);
							if (row == table.getModel().getRowCount() - 1) {
		
							}
						}
					});
					classCombo.addItemListener(getItemListener());
					classCombo.setEnabled(false);
					classroomData.fillClassComboBox(selectedYear, classCombo, progressBar);
					classCombo.setEnabled(true);
					possibleClasses.put(selectedYear, classCombo);
				}
				return possibleClasses.get(selectedYear);
			}
			return null;
		}
	}
	class AssignmentColumnEditor extends ClassInfoColumnEditor {
		private MainClassroomData classroomData;	
		private Map<String, JComboBox<ClassroomInfo>> possibleAssignments;
		
		public AssignmentColumnEditor(MainClassroomData data, JProgressBar progressBar) {
			super(progressBar);
			classroomData = data;			
			possibleAssignments = new HashMap<String, JComboBox<ClassroomInfo>>();			
		}


		@Override
		public JComboBox<ClassroomInfo> getCurrentCombo(JTable table, int row, int column) {
			ClassroomInfo selectedClass = (ClassroomInfo)table.getModel().getValueAt(row, 1);
			if (selectedClass != null && selectedClass.getId() != null) {
				if (possibleAssignments.containsKey(selectedClass.getId()) == false) {
					
					JComboBox<ClassroomInfo> assignmentCombo = new JComboBox<ClassroomInfo>();
					assignmentCombo.setEnabled(false);
					classroomData.fillAssignmentComboBox(selectedClass, assignmentCombo, progressBar, true);
					possibleAssignments.put(selectedClass.getId(), assignmentCombo);
					assignmentCombo.setEnabled(true);
					assignmentCombo.addItemListener(getItemListener());
				}
				return possibleAssignments.get(selectedClass.getId());
			}
			return null;
		}
	}

}
