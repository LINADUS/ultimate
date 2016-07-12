package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.congruence;

import de.uni_freiburg.informatik.ultimate.boogie.IBoogieVar;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.boogie.symboltable.BoogieSymbolTable;
import de.uni_freiburg.informatik.ultimate.core.model.preferences.IPreferenceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.model.IAbstractDomain;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.model.IAbstractPostOperator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.model.IAbstractStateBinaryOperator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.model.IEqualityProvider;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.util.DefaultEqualityProvider;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.preferences.AbsIntPrefInitializer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RootAnnot;

/**
 * 
 * @author Frank Schüssele (schuessf@informatik.uni-freiburg.de)
 * @author Marius Greitschus (greitsch@informatik.uni-freiburg.de)
 *
 */

public class CongruenceDomain implements IAbstractDomain<CongruenceDomainState, CodeBlock, IBoogieVar, Expression> {

	private final BoogieSymbolTable mSymbolTable;
	private final ILogger mLogger;
	private final IUltimateServiceProvider mServices;
	private final RootAnnot mRootAnnotation;

	private IAbstractStateBinaryOperator<CongruenceDomainState> mWideningOperator;
	private IAbstractStateBinaryOperator<CongruenceDomainState> mMergeOperator;
	private IAbstractPostOperator<CongruenceDomainState, CodeBlock, IBoogieVar> mPostOperator;
	private IEqualityProvider<CongruenceDomainState, CodeBlock, IBoogieVar, Expression> mEqualityProvider;

	public CongruenceDomain(final ILogger logger, final IUltimateServiceProvider services,
			final BoogieSymbolTable symbolTable, final RootAnnot rootAnnotation) {
		mLogger = logger;
		mSymbolTable = symbolTable;
		mServices = services;
		mRootAnnotation = rootAnnotation;
	}

	@Override
	public CongruenceDomainState createFreshState() {
		return new CongruenceDomainState(mLogger);
	}

	@Override
	public IAbstractStateBinaryOperator<CongruenceDomainState> getWideningOperator() {
		if (mWideningOperator == null) {
			// Widening is the same as merge, so we don't need an extra operator
			mWideningOperator = new CongruenceMergeOperator();
		}
		return mWideningOperator;
	}

	@Override
	public IAbstractStateBinaryOperator<CongruenceDomainState> getMergeOperator() {
		if (mMergeOperator == null) {
			mMergeOperator = new CongruenceMergeOperator();
		}
		return mMergeOperator;
	}

	@Override
	public IAbstractPostOperator<CongruenceDomainState, CodeBlock, IBoogieVar> getPostOperator() {
		if (mPostOperator == null) {
			final IPreferenceProvider prefs = mServices.getPreferenceProvider(Activator.PLUGIN_ID);
			final String evaluatorType = prefs.getString(CongruenceDomainPreferences.LABEL_EVALUATOR_TYPE);
			final int maxParallelStates = prefs.getInt(AbsIntPrefInitializer.LABEL_MAX_PARALLEL_STATES);
			final CongruenceDomainStatementProcessor stmtProcessor = new CongruenceDomainStatementProcessor(mLogger,
					mSymbolTable, mRootAnnotation.getBoogie2SMT().getBoogie2SmtSymbolTable(), evaluatorType,
					maxParallelStates);
			mPostOperator = new CongruencePostOperator(mLogger, mSymbolTable, stmtProcessor);
		}
		return mPostOperator;
	}

	@Override
	public int getDomainPrecision() {
		return 300;
	}

	@Override
	public IEqualityProvider<CongruenceDomainState, CodeBlock, IBoogieVar, Expression> getEqualityProvider() {
		if (mEqualityProvider == null) {
			mEqualityProvider = new DefaultEqualityProvider<>(mPostOperator, mRootAnnotation);
		}
		return mEqualityProvider;
	}
}
