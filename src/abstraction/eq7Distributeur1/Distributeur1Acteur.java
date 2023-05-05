package abstraction.eq7Distributeur1;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import abstraction.eqXRomu.contratsCadres.SuperviseurVentesContratCadre;
import abstraction.eqXRomu.filiere.Filiere;
import abstraction.eqXRomu.filiere.IActeur;
import abstraction.eqXRomu.general.Journal;
import abstraction.eqXRomu.general.Variable;
import abstraction.eqXRomu.general.VariablePrivee;
import abstraction.eqXRomu.produits.Chocolat;
import abstraction.eqXRomu.produits.ChocolatDeMarque;
import abstraction.eqXRomu.produits.Gamme;

public class Distributeur1Acteur  implements IActeur {
	////////////////////////////////////////////////
	//declaration des variables
	public static Color COLOR_LLGRAY = new Color(238,238,238);
	public static Color COLOR_BROWN  = new Color(141,100,  7);
	public static Color COLOR_PURPLE = new Color(100, 10,115);
	public static Color COLOR_LPURPLE= new Color(155, 89,182);
	public static Color COLOR_GREEN  = new Color(  6,162, 37);
	public static Color COLOR_LGREEN = new Color(  6,255, 37);
	public static Color COLOR_LBLUE  = new Color(  6,130,230);
	
	protected Journal journal;
	protected Journal journal_achat;
	protected Journal journal_stock;
	protected Journal journal_vente;
	
	
	//On est oblige de mettre les variables ici sinon la creation de la filiere est dans un tel ordre que nous n'y avons pas acces assez tot
	protected Variable totalStocks = new VariablePrivee("Eq7TotalStocks", "<html>Quantite totale de chocolat (de marque) en stock</html>",this, 0.0, 1000000.0, 0.0);
	//La quantité totale de stock de chocolat 
	protected Variable stock_BQ = new Variable("Eq7stock_BQ", "Stock total de chocolat de basse qualité", this, 0);
	protected Variable stock_MQ = new Variable("Eq7stock_MQ", "Stock total de chocolat de moyenne qualité", this, 0);
	protected Variable stock_MQ_BE = new Variable("Eq7stock_MQ_BE", "stock Total de chocolat de moyenne qualité bio-équitable", this, 0);
	protected Variable stock_HQ_BE = new Variable("Eq7stock_HQ_BE", "stock Total de chocolat de haute qualité bio-équitable", this, 0);
	protected Variable ventes = new Variable("Eq7ventes","ventes totales réalisées lors de ce tour",this,0);
	/**
	 * donne les quantités mini pour un contrat cadre
	 * @author ghaly
	 */
	double quantite_min_cc = SuperviseurVentesContratCadre.QUANTITE_MIN_ECHEANCIER;
	
	/**
	 * previsions de ventes de la filiere globale pour chaque etape_normalisee
	 * prevision etape -> marque -> valeur
	 */
	
	/**
	 * previsions de vente de l'equipe 7
	 * on suppose qu'on vend à chaque étape
	 * prevision etape -> marque -> valeur
	 */
	protected HashMap<Integer,HashMap<ChocolatDeMarque,Double>> previsionsperso; 
	
	/**
	 * couts: couts d'achat à travers les contrats cadres
	 */
	//protected HashMap<ChocolatDeMarque,Double> moyenne_couts = new HashMap<ChocolatDeMarque,Double>(); 
	protected HashMap<ChocolatDeMarque,Double> cout_marque = new HashMap<ChocolatDeMarque,Double>(); 
	
	/**
	 * Cout en fonction du chocolat, pour 1t
	 */
	protected HashMap<Chocolat,Double> cout_chocolat = new HashMap<Chocolat,Double>();
	
	/**
	 * nombre d'achat en contrat cadre, ça servira à calculer la moyenne des couts
	 */
	protected HashMap<ChocolatDeMarque,Integer> nombre_achats = new HashMap<ChocolatDeMarque,Integer>();; 

	protected Variable cout_stockage_distributeur = new Variable("cout moyen stockage distributeur", this);
	protected Variable cout_main_doeuvre_distributeur = new Variable("cout de la main d'oeuvre pour les distributeur", this); //cout total par tour

	
	protected LinkedList<VariablePrivee> liste = new LinkedList<VariablePrivee>();
	protected int cryptogramme;
	

	public Distributeur1Acteur() {
		this.journal = new Journal("Journal "+this.getNom(), this);
	    this.journal_achat=new Journal("Journal des Achats de l'" + this.getNom(),this);
	    this.journal_stock = new Journal("Journal des Stocks de l'" + this.getNom(),this);
	    this.journal_vente = new Journal("Journal des ventes de l'" + this.getNom(),this);
	    
	}
	

	
	////////////////////////////////////////////////////////
	//         Methodes principales				          //
	////////////////////////////////////////////////////////
	

	/**
	 * @author Theo
	 * Renvoie les previsions de vente de notre quipe, actualisees à chaque tour
	 */
	protected double getPrevisionsperso(ChocolatDeMarque marque, Integer etape) {
		return previsionsperso.get(etape).get(marque);
	}
	
	/**
	 * @author Theo
	 * @return le prix de la gamme associée à marque (par tonne)
	 */
	protected double getCout_gamme(ChocolatDeMarque marque) {
		Chocolat gamme = marque.getChocolat();
		return cout_chocolat.get(gamme);
	}

	/**
	 * Actualisation des previsions persos
	 * @author Theo, Ghaly
	 */
	public void actualiser_prevision_perso(ChocolatDeMarque choco,  double quantite) {
		int etape_annee = Filiere.LA_FILIERE.getEtape()/24+1;
		int etapenormalisee = Filiere.LA_FILIERE.getEtape()%24;
		HashMap<ChocolatDeMarque,Double> prevetapeperso = previsionsperso.get(etapenormalisee);
		prevetapeperso.replace(choco, (prevetapeperso.get(choco)*etape_annee+quantite)/(etape_annee+1));
		previsionsperso.replace(etapenormalisee, prevetapeperso);
	}
	
	/**
	 * @author Theo and Ghaly
	 */
	public void initialiser() {
		cout_stockage_distributeur.setValeur(this, Filiere.LA_FILIERE.getParametre("cout moyen stockage producteur").getValeur()*16);
		cout_chocolat.put(Chocolat.C_HQ_BE, 10000.);
		cout_chocolat.put(Chocolat.C_MQ_BE, 7500.);
		cout_chocolat.put(Chocolat.C_MQ, 5000.);
		cout_chocolat.put(Chocolat.C_BQ, 3000.);
		
		
		/////////////////////////////////////
		//POTENTIELLEMENT à Changer
		cout_main_doeuvre_distributeur.setValeur(this, 1);
		///////////////////////////////////////
		
		//Initialisation des couts
		for (ChocolatDeMarque marque : Filiere.LA_FILIERE.getChocolatsProduits()) {
			cout_marque.put(marque, getCout_gamme(marque));
		}
		//Initialisation des previsions
		this.previsionsperso = new HashMap<Integer,HashMap<ChocolatDeMarque,Double>>(); 
		
		for (int i=0;i<24;i++) {
			HashMap<ChocolatDeMarque,Double> prevtour = new HashMap<ChocolatDeMarque,Double>();
			HashMap<ChocolatDeMarque,Double> prevtourperso = new HashMap<ChocolatDeMarque,Double>();
			for (ChocolatDeMarque marque : Filiere.LA_FILIERE.getChocolatsProduits()) {
				prevtour.put(marque, Filiere.LA_FILIERE.getVentes(marque, -(i+1)));
				prevtourperso.put(marque, Filiere.LA_FILIERE.getVentes(marque, -(i+1))*0.5);
				//Pour l'initialisation, on estime vendre 50% des ventes totales (choix arbitraire pour démarrer
			}
			previsionsperso.put(24-(i+1), prevtourperso);
		}
	}

	public String getNom() {
		return "EQ7";
	}
	
	////////////////////////////////////////////////////////
	//         En lien avec l'interface graphique         //
	////////////////////////////////////////////////////////
	public String toString() {
		return this.getNom();
		}
	

	/**
	 * @author Romain,Ghaly et Theo
	 */
	public void next() {
		
		int etape = Filiere.LA_FILIERE.getEtape();
		
		for (ChocolatDeMarque marque : Filiere.LA_FILIERE.getChocolatsProduits()) {
			actualiser_prevision_perso( marque,  etape);
		}
	}

	public Color getColor() {// NE PAS MODIFIER
		return new Color(162, 207, 238); 
	}

	public String getDescription() {
		return "Bla bla bla";
	}


	/**
	 * Renvoie les indicateurs
	 * @author Ghaly 
	 */
	public List<Variable> getIndicateurs() {
		
		List<Variable> res = new ArrayList<Variable>();
		res.add(totalStocks);
		res.add(stock_HQ_BE);
		res.add(stock_MQ_BE);
		res.add(stock_BQ);
		res.add(stock_MQ);
		res.add(ventes);
		
		return res;

	}

	public List<Variable> getParametres() {
		List<Variable> res=new ArrayList<Variable>();
		res.add(cout_stockage_distributeur);
		return res;
	}

	public List<Journal> getJournaux() {
		List<Journal> res=new ArrayList<Journal>();
		res.add(this.journal);
		res.add(this.journal_achat);
		res.add(this.journal_stock);
		res.add(this.journal_vente);
		return res;
	}

	////////////////////////////////////////////////////////
	//               En lien avec la Banque               //
	////////////////////////////////////////////////////////

/**
 * Methode appelee par la banque apres la creation du compte bancaire de l'acteur afin de lui communiquer le cryptogramme
 *  qui lui sera necessaire pour les operations bancaires
 */
	public void setCryptogramme(Integer crypto) {
		this.cryptogramme = crypto;

	}
	
	/**
	 * Appelee lorsqu'un acteur fait faillite (potentiellement vous afin de vous en informer.
	 * @author Ghaly 
	 */
	public void notificationFaillite(IActeur acteur) {
		if (this==acteur) {
			System.out.println("They killed Chocorama... ");
		} else {
			System.out.println("try again "+acteur.getNom()+"... We ("+this.getNom()+") will not miss you.");
		}
	}

	/**
	 * Apres chaque operation sur votre compte bancaire, cette operation est appelee pour vous en informer
	 */
	public void notificationOperationBancaire(double montant) {
	}
	
	/**
	 *  Renvoie le solde actuel de l'acteur
	 */
	public double getSolde() {
		return Filiere.LA_FILIERE.getBanque().getSolde(Filiere.LA_FILIERE.getActeur(getNom()), this.cryptogramme);
	}

	////////////////////////////////////////////////////////
	//        Pour la creation de filieres de test        //
	////////////////////////////////////////////////////////

	/**
	 *  Renvoie la liste des filieres proposees par l'acteur
	 */
	public List<String> getNomsFilieresProposees() {
		ArrayList<String> filieres = new ArrayList<String>();
		return(filieres);
	}

	/**
	 *  Renvoie une instance d'une filiere d'apres son nom
	 */
	public Filiere getFiliere(String nom) {
		return Filiere.LA_FILIERE;
	}

}
