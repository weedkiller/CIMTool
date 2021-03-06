/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import static au.com.langdale.ui.builder.Templates.CheckBox;
import static au.com.langdale.ui.builder.Templates.Field;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;

import au.com.langdale.ui.binding.Validator;
import au.com.langdale.ui.builder.Assembly;
import au.com.langdale.ui.builder.Template;
import au.com.langdale.ui.plumbing.Binding;
import au.com.langdale.ui.plumbing.Observer;

/**
 * A preference or property page provided with a Assembly and
 * a set of templates for various property and preference types.
 */
public abstract class FurnishedPropertyPage extends PreferencePage 
			implements Observer, IWorkbenchPreferencePage, IWorkbenchPropertyPage {
	
	private Content content;
	private Control body;
	private IResource resource;
	
	public FurnishedPropertyPage(String title, ImageDescriptor image) {
		super(title, image);
	}

	public void init(IWorkbench workbench) {

	}

	public FurnishedPropertyPage(String title) {
		super(title);
	}

	public FurnishedPropertyPage() {
	}

	public static abstract class TextBinding implements Template, Binding {
		public final QualifiedName symbol;
		public final Validator validator;
		private Assembly assembly;

		public TextBinding(QualifiedName symbol, Validator validator) {
			this.symbol = symbol;
			this.validator = validator;
		}

		protected String getValue() {
			return assembly.getText(symbol.getLocalName()).getText().trim();
		}

		protected void setValue(String value) {
			assembly.setTextValue(symbol.getLocalName(), value);
		}

		public String validate() {
			return validator.validate(getValue());
		}

		public Control realise(Composite parent, Assembly assembly) {
			this.assembly = assembly;
			assembly.addBinding(this);
			return Field(symbol.getLocalName()).realise(parent, assembly);
		}
	}
	
	public static abstract class OptionBinding implements Template, Binding {
		public final QualifiedName symbol;
		public final String label;
		private Assembly assembly;

		public OptionBinding(QualifiedName symbol, String label) {
			this.symbol = symbol;
			this.label = label;
		}

		protected boolean getValue() {
			return assembly.getButton(symbol.getLocalName()).getSelection();
		}

		protected void setValue(boolean value) {
			assembly.getButton(symbol.getLocalName()).setSelection(value);
		}

		public String validate() {
			return null;
		}

		public Control realise(Composite parent, Assembly assembly) {
			this.assembly = assembly;
			assembly.addBinding(this);
			return CheckBox(symbol.getLocalName(), label).realise(parent, assembly);
		}
	}
	
	protected class Property extends TextBinding {
		public Property(QualifiedName symbol, Validator validator) {
			super( symbol, validator );
		}

		public void refresh() {
			try {
				setValue(Info.getProperty(getResource(), symbol));
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void update() {
			Info.putProperty(getResource(), symbol, getValue());
		}

		public void reset() {
			setValue(getPreferenceStore().getString(symbol.getLocalName()));				
		}
	}
	
	protected class Preference extends TextBinding {
		public Preference(QualifiedName symbol, Validator validator) {
			super(symbol, validator);
		}

		public void refresh() {
			setValue(getPreferenceStore().getString(symbol.getLocalName()));		
		}

		public void reset() {
			setValue(getPreferenceStore().getDefaultString(symbol.getLocalName()));		
		}

		public void update() {
			getPreferenceStore().setValue(symbol.getLocalName(), getValue());
		}
	}
	
	protected class PreferenceOption extends OptionBinding {
		public PreferenceOption(QualifiedName symbol, String label) {
			super(symbol, label);
		}

		public void refresh() {
			setValue(getPreferenceStore().getBoolean(symbol.getLocalName()));		
		}

		public void reset() {
			setValue(getPreferenceStore().getDefaultBoolean(symbol.getLocalName()));		
		}

		public void update() {
			getPreferenceStore().setValue(symbol.getLocalName(), getValue());
		}
	}

	
	protected abstract class Content extends Assembly {
		public Content() {
			super(createDialogToolkit(), FurnishedPropertyPage.this, false);
		}
		
		protected abstract Template define();
		protected void addBindings() {}
	}
	
	protected abstract Content createContent();

	@Override
	protected final Control createContents(Composite parent) {
		content = createContent();
		body = content.realise(parent, content.define());
		content.addBindings();
		content.doRefresh(); 
		return body;
	}

	public void markInvalid(String message) {
		setErrorMessage(message);
		setValid(false);
	}

	public void markValid() {
		setErrorMessage(null);
		setValid(true);
	}
	
	public void markDirty() {
		
	}
	
	@Override
	public boolean performOk() {
		content.fireUpdate();
		return true;
	}

	@Override
	protected void performDefaults() {
		content.doReset();
		super.performDefaults();
	}

	public IAdaptable getElement() {
		return resource;
	}

	public IResource getResource() {
		return resource;
	}

	public void setElement(IAdaptable element) {
		resource = (IResource) element;
		
	}
}
