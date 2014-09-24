package cz.brmlab.yodaqa.analysis.passage;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.brmlab.yodaqa.model.Question.Clue;
import cz.brmlab.yodaqa.model.SearchResult.CandidateAnswer;
import cz.brmlab.yodaqa.model.SearchResult.Passage;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;

/**
 * Create CandidateAnswers for all NEs (named entities) that do not
 * contain supplied clues.
 *
 * This is pretty naive but should generate some useful answers. */

@SofaCapability(
	inputSofas = { "Question", "PickedPassages" },
	outputSofas = { "PickedPassages" }
)

public class CanByNESurprise extends JCasAnnotator_ImplBase {
	final Logger logger = LoggerFactory.getLogger(CanByNESurprise.class);

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas questionView, passagesView;
		try {
			questionView = jcas.getView("Question");
			passagesView = jcas.getView("PickedPassages");
		} catch (CASException e) {
			throw new AnalysisEngineProcessException(e);
		}

		for (Passage p: JCasUtil.select(passagesView, Passage.class)) {
			for (NamedEntity ne : JCasUtil.selectCovered(NamedEntity.class, p)) {
				String text = ne.getCoveredText();

				/* TODO: This can be optimized a lot. */
				boolean matches = false;
				for (Clue clue : JCasUtil.select(questionView, Clue.class)) {
					if (text.endsWith(clue.getLabel())) {
						matches = true;
						break;
					}
				}
				if (matches)
					continue;

				/* Surprise! */

				logger.info("caNE {}", ne.getCoveredText());

				CandidateAnswer ca = new CandidateAnswer(passagesView);
				ca.setBegin(ne.getBegin());
				ca.setEnd(ne.getEnd());
				ca.setPassage(p);
				ca.setBase(ne);
				ca.setConfidence(1.0);
				ca.addToIndexes();
			}
		}
	}
}