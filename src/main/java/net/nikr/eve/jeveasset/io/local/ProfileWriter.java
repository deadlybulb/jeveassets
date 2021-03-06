/*
 * Copyright 2009-2016 Contributors (see credits.txt)
 *
 * This file is part of jEveAssets.
 *
 * jEveAssets is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * jEveAssets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jEveAssets; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package net.nikr.eve.jeveasset.io.local;

import com.beimin.eveapi.model.shared.Blueprint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.nikr.eve.jeveasset.data.MyAccount;
import net.nikr.eve.jeveasset.data.MyAccountBalance;
import net.nikr.eve.jeveasset.data.Owner;
import net.nikr.eve.jeveasset.data.ProfileManager;
import net.nikr.eve.jeveasset.gui.tabs.assets.MyAsset;
import net.nikr.eve.jeveasset.gui.tabs.contracts.MyContract;
import net.nikr.eve.jeveasset.gui.tabs.contracts.MyContractItem;
import net.nikr.eve.jeveasset.gui.tabs.jobs.MyIndustryJob;
import net.nikr.eve.jeveasset.gui.tabs.journal.MyJournal;
import net.nikr.eve.jeveasset.gui.tabs.orders.MyMarketOrder;
import net.nikr.eve.jeveasset.gui.tabs.transaction.MyTransaction;
import net.nikr.eve.jeveasset.io.shared.AbstractXmlWriter;
import net.nikr.eve.jeveasset.io.shared.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public final class ProfileWriter extends AbstractXmlWriter {

	private static final Logger LOG = LoggerFactory.getLogger(ProfileWriter.class);

	private ProfileWriter() { }

	public static boolean save(final ProfileManager profileManager, final String filename) {
		ProfileWriter writer = new ProfileWriter();
		return writer.write(profileManager, filename);
	}

	private boolean write(final ProfileManager profileManager, final String filename) {
		Document xmldoc;
		try {
			xmldoc = getXmlDocument("assets");
		} catch (XmlException ex) {
			LOG.error("Profile not saved " + ex.getMessage(), ex);
			return false;
		}
		writeAccounts(xmldoc, profileManager.getAccounts());
		try {
			writeXmlFile(xmldoc, filename, true);
		} catch (XmlException ex) {
			LOG.error("Profile not saved " + ex.getMessage(), ex);
			return false;
		}
		LOG.info("Profile saved");
		return true;
	}

	private void writeAccounts(final Document xmldoc, final List<MyAccount> accounts) {
		Element parentNode = xmldoc.createElementNS(null, "accounts");
		xmldoc.getDocumentElement().appendChild(parentNode);

		for (MyAccount account : accounts) {
			Element node = xmldoc.createElementNS(null, "account");
			node.setAttributeNS(null, "keyid", String.valueOf(account.getKeyID()));
			node.setAttributeNS(null, "vcode", account.getVCode());
			node.setAttributeNS(null, "name", account.getName());
			node.setAttributeNS(null, "charactersnextupdate", String.valueOf(account.getAccountNextUpdate().getTime()));
			node.setAttributeNS(null, "accessmask", String.valueOf(account.getAccessMask()));
			if (account.getType() != null) {
				node.setAttributeNS(null, "type", account.getType().name());
			}
			node.setAttributeNS(null, "expires", account.getExpires() == null ? "0" : String.valueOf(account.getExpires().getTime()));
			node.setAttributeNS(null, "invalid", String.valueOf(account.isInvalid()));
			parentNode.appendChild(node);
			writeOwners(xmldoc, node, account.getOwners());

		}
	}

	private void writeOwners(final Document xmldoc, final Element parentNode, final List<Owner> owners) {
		for (Owner owner : owners) {
			Element node = xmldoc.createElementNS(null, "human");
			node.setAttributeNS(null, "id", String.valueOf(owner.getOwnerID()));
			node.setAttributeNS(null, "name", owner.getName());
			node.setAttributeNS(null, "show", String.valueOf(owner.isShowOwner()));
			if (owner.getAssetLastUpdate() != null) {
				node.setAttributeNS(null, "assetslastupdate", String.valueOf(owner.getAssetLastUpdate().getTime()));
			}
			node.setAttributeNS(null, "assetsnextupdate", String.valueOf(owner.getAssetNextUpdate().getTime()));
			if (owner.getBalanceLastUpdate() != null) {
				node.setAttributeNS(null, "balancelastupdate", String.valueOf(owner.getBalanceLastUpdate().getTime()));
			}
			node.setAttributeNS(null, "balancenextupdate", String.valueOf(owner.getBalanceNextUpdate().getTime()));
			node.setAttributeNS(null, "marketordersnextupdate", String.valueOf(owner.getMarketOrdersNextUpdate().getTime()));
			node.setAttributeNS(null, "journalnextupdate", String.valueOf(owner.getJournalNextUpdate().getTime()));
			node.setAttributeNS(null, "wallettransactionsnextupdate", String.valueOf(owner.getTransactionsNextUpdate().getTime()));
			node.setAttributeNS(null, "industryjobsnextupdate", String.valueOf(owner.getIndustryJobsNextUpdate().getTime()));
			node.setAttributeNS(null, "contractsnextupdate", String.valueOf(owner.getContractsNextUpdate().getTime()));
			node.setAttributeNS(null, "locationsnextupdate", String.valueOf(owner.getLocationsNextUpdate().getTime()));
			node.setAttributeNS(null, "blueprintsnextupdate", String.valueOf(owner.getBlueprintsNextUpdate().getTime()));
			parentNode.appendChild(node);
			Element childNode = xmldoc.createElementNS(null, "assets");
			node.appendChild(childNode);
			writeAssets(xmldoc, childNode, owner.getAssets());
			writeContractItems(xmldoc, node, owner.getContracts());
			writeAccountBalances(xmldoc, node, owner.getAccountBalances(), owner.isCorporation());
			writeMarketOrders(xmldoc, node, owner.getMarketOrders(), owner.isCorporation());
			writeJournals(xmldoc, node, new ArrayList<MyJournal>(owner.getJournal()), owner.isCorporation());
			writeTransactions(xmldoc, node, new ArrayList<MyTransaction>(owner.getTransactions()), owner.isCorporation());
			writeIndustryJobs(xmldoc, node, owner.getIndustryJobs(), owner.isCorporation());
			writeBlueprints(xmldoc, node, owner.getBlueprints(), owner.isCorporation());
		}
	}

	private void writeAssets(final Document xmldoc, final Element parentNode, final List<MyAsset> assets) {
		for (MyAsset asset : assets) {
			Element node = xmldoc.createElementNS(null, "asset");
			node.setAttributeNS(null, "count", String.valueOf(asset.getCount()));
			node.setAttributeNS(null, "flagid", String.valueOf(asset.getFlagID()));
			node.setAttributeNS(null, "id", String.valueOf(asset.getItemID()));
			node.setAttributeNS(null, "typeid", String.valueOf(asset.getItem().getTypeID()));
			node.setAttributeNS(null, "locationid", String.valueOf(asset.getLocation().getLocationID()));
			node.setAttributeNS(null, "singleton", String.valueOf(asset.isSingleton()));
			node.setAttributeNS(null, "rawquantity", String.valueOf(asset.getRawQuantity()));
			parentNode.appendChild(node);
			writeAssets(xmldoc, node, asset.getAssets());
		}
	}

	private void writeContractItems(Document xmldoc, Element parentNode, Map<MyContract, List<MyContractItem>> contractItems) {
		Element contractsNode = xmldoc.createElementNS(null, "contracts");
		parentNode.appendChild(contractsNode);
		for (Map.Entry<MyContract, List<MyContractItem>> entry : contractItems.entrySet()) {
			MyContract contract = entry.getKey();
			Element contractNode = xmldoc.createElementNS(null, "contract");
			contractNode.setAttributeNS(null, "acceptorid", String.valueOf(contract.getAcceptorID()));
			contractNode.setAttributeNS(null, "assigneeid", String.valueOf(contract.getAssigneeID()));
			contractNode.setAttributeNS(null, "availability", contract.getAvailability().name());
			contractNode.setAttributeNS(null, "buyout", String.valueOf(contract.getBuyout()));
			contractNode.setAttributeNS(null, "collateral", String.valueOf(contract.getCollateral()));
			contractNode.setAttributeNS(null, "contractid", String.valueOf(contract.getContractID()));
			if (contract.getDateAccepted() != null) {
				contractNode.setAttributeNS(null, "dateaccepted", String.valueOf(contract.getDateAccepted().getTime()));
			}
			if (contract.getDateCompleted() != null) {
				contractNode.setAttributeNS(null, "datecompleted", String.valueOf(contract.getDateCompleted().getTime()));
			}
			contractNode.setAttributeNS(null, "dateexpired", String.valueOf(contract.getDateExpired().getTime()));
			contractNode.setAttributeNS(null, "dateissued", String.valueOf(contract.getDateIssued().getTime()));
			contractNode.setAttributeNS(null, "endstationid", String.valueOf(contract.getEndStationID()));
			contractNode.setAttributeNS(null, "issuercorpid", String.valueOf(contract.getIssuerCorpID()));
			contractNode.setAttributeNS(null, "issuerid", String.valueOf(contract.getIssuerID()));
			contractNode.setAttributeNS(null, "numdays", String.valueOf(contract.getNumDays()));
			contractNode.setAttributeNS(null, "price", String.valueOf(contract.getPrice()));
			contractNode.setAttributeNS(null, "reward", String.valueOf(contract.getReward()));
			contractNode.setAttributeNS(null, "startstationid", String.valueOf(contract.getStartStationID()));
			contractNode.setAttributeNS(null, "status", contract.getStatus().name());
			contractNode.setAttributeNS(null, "title", String.valueOf(contract.getTitle()));
			contractNode.setAttributeNS(null, "type", contract.getType().name());
			contractNode.setAttributeNS(null, "volume", String.valueOf(contract.getVolume()));
			contractNode.setAttributeNS(null, "forcorp", String.valueOf(contract.isForCorp()));
			contractsNode.appendChild(contractNode);
			for (MyContractItem contractItem : entry.getValue()) {
				Element itemNode = xmldoc.createElementNS(null, "contractitem");
				itemNode.setAttributeNS(null, "included", String.valueOf(contractItem.isIncluded()));
				itemNode.setAttributeNS(null, "quantity", String.valueOf(contractItem.getQuantity()));
				itemNode.setAttributeNS(null, "recordid", String.valueOf(contractItem.getRecordID()));
				itemNode.setAttributeNS(null, "singleton", String.valueOf(contractItem.isSingleton()));
				itemNode.setAttributeNS(null, "typeid", String.valueOf(contractItem.getTypeID()));
				if (contractItem.getRawQuantity() != null) {
					itemNode.setAttributeNS(null, "rawquantity", String.valueOf(contractItem.getRawQuantity()));
				}
				contractNode.appendChild(itemNode);
			}
		}
	}

	private void writeAccountBalances(final Document xmldoc, final Element parentNode, final List<MyAccountBalance> accountBalances, final boolean bCorp) {
		Element node = xmldoc.createElementNS(null, "balances");
		if (!accountBalances.isEmpty()) {
			node.setAttributeNS(null, "corp", String.valueOf(bCorp));
			parentNode.appendChild(node);
		}
		for (MyAccountBalance accountBalance : accountBalances) {
			Element childNode = xmldoc.createElementNS(null, "balance");
			childNode.setAttributeNS(null, "accountid", String.valueOf(accountBalance.getAccountID()));
			childNode.setAttributeNS(null, "accountkey", String.valueOf(accountBalance.getAccountKey()));
			childNode.setAttributeNS(null, "balance", String.valueOf(accountBalance.getBalance()));
			node.appendChild(childNode);
		}
	}

	private void writeMarketOrders(final Document xmldoc, final Element parentNode, final List<MyMarketOrder> marketOrders, final boolean bCorp) {
		Element node = xmldoc.createElementNS(null, "markerorders");
		if (!marketOrders.isEmpty()) {
			node.setAttributeNS(null, "corp", String.valueOf(bCorp));
			parentNode.appendChild(node);
		}
		for (MyMarketOrder marketOrder : marketOrders) {
			Element childNode = xmldoc.createElementNS(null, "markerorder");
			childNode.setAttributeNS(null, "orderid", String.valueOf(marketOrder.getOrderID()));
			childNode.setAttributeNS(null, "charid", String.valueOf(marketOrder.getCharID()));
			childNode.setAttributeNS(null, "stationid", String.valueOf(marketOrder.getStationID()));
			childNode.setAttributeNS(null, "volentered", String.valueOf(marketOrder.getVolEntered()));
			childNode.setAttributeNS(null, "volremaining", String.valueOf(marketOrder.getVolRemaining()));
			childNode.setAttributeNS(null, "minvolume", String.valueOf(marketOrder.getMinVolume()));
			childNode.setAttributeNS(null, "orderstate", String.valueOf(marketOrder.getOrderState()));
			childNode.setAttributeNS(null, "typeid", String.valueOf(marketOrder.getTypeID()));
			childNode.setAttributeNS(null, "range", String.valueOf(marketOrder.getRange()));
			childNode.setAttributeNS(null, "accountkey", String.valueOf(marketOrder.getAccountKey()));
			childNode.setAttributeNS(null, "duration", String.valueOf(marketOrder.getDuration()));
			childNode.setAttributeNS(null, "escrow", String.valueOf(marketOrder.getEscrow()));
			childNode.setAttributeNS(null, "price", String.valueOf(marketOrder.getPrice()));
			childNode.setAttributeNS(null, "bid", String.valueOf(marketOrder.getBid()));
			childNode.setAttributeNS(null, "issued", String.valueOf(marketOrder.getIssued().getTime()));
			node.appendChild(childNode);
		}
	}

	private void writeJournals(final Document xmldoc, final Element parentNode, final List<MyJournal> journals, final boolean bCorp) {
		Element node = xmldoc.createElementNS(null, "journals");
		if (!journals.isEmpty()) {
			node.setAttributeNS(null, "corp", String.valueOf(bCorp));
			parentNode.appendChild(node);
		}
		for (MyJournal journal : journals) {
			Element childNode = xmldoc.createElementNS(null, "journal");
			//Base
			childNode.setAttributeNS(null, "amount", String.valueOf(journal.getAmount()));
			childNode.setAttributeNS(null, "argid1", String.valueOf(journal.getArgID1()));
			childNode.setAttributeNS(null, "argname1", String.valueOf(journal.getArgName1()));
			childNode.setAttributeNS(null, "balance", String.valueOf(journal.getBalance()));
			childNode.setAttributeNS(null, "date", String.valueOf(journal.getDate().getTime()));
			childNode.setAttributeNS(null, "ownerid1", String.valueOf(journal.getOwnerID1()));
			childNode.setAttributeNS(null, "ownerid2", String.valueOf(journal.getOwnerID2()));
			childNode.setAttributeNS(null, "ownername1", journal.getOwnerName1());
			childNode.setAttributeNS(null, "ownername2", journal.getOwnerName2());
			childNode.setAttributeNS(null, "reason", journal.getReason());
			childNode.setAttributeNS(null, "refid", String.valueOf(journal.getRefID()));
			childNode.setAttributeNS(null, "reftypeid", String.valueOf(journal.getRefTypeID()));
			if (journal.getTaxAmount() != null) {
				childNode.setAttributeNS(null, "taxamount", String.valueOf(journal.getTaxAmount()));
			}
			if (journal.getTaxReceiverID() != null) {
				childNode.setAttributeNS(null, "taxreceiverid", String.valueOf(journal.getTaxReceiverID()));
			}
			//Extra
			childNode.setAttributeNS(null, "accountkey", String.valueOf(journal.getAccountKey()));
			node.appendChild(childNode);
		}
	}

	private void writeTransactions(final Document xmldoc, final Element parentNode, final List<MyTransaction> transactions, final boolean bCorp) {
		Element node = xmldoc.createElementNS(null, "wallettransactions");
		if (!transactions.isEmpty()) {
			node.setAttributeNS(null, "corp", String.valueOf(bCorp));
			parentNode.appendChild(node);
		}
		for (MyTransaction transaction : transactions) {
			Element childNode = xmldoc.createElementNS(null, "wallettransaction");
			childNode.setAttributeNS(null, "transactiondatetime", String.valueOf(transaction.getTransactionDateTime().getTime()));
			childNode.setAttributeNS(null, "transactionid", String.valueOf(transaction.getTransactionID()));
			childNode.setAttributeNS(null, "quantity", String.valueOf(transaction.getQuantity()));
			childNode.setAttributeNS(null, "typename", String.valueOf(transaction.getTypeName()));
			childNode.setAttributeNS(null, "typeid", String.valueOf(transaction.getTypeID()));
			childNode.setAttributeNS(null, "price", String.valueOf(transaction.getPrice()));
			childNode.setAttributeNS(null, "clientid", String.valueOf(transaction.getClientID()));
			childNode.setAttributeNS(null, "clientname", String.valueOf(transaction.getClientName()));
			if (transaction.getCharacterID() != null) {
				childNode.setAttributeNS(null, "characterid", String.valueOf(transaction.getCharacterID()));
			}
			if (transaction.getCharacterName() != null) {
				childNode.setAttributeNS(null, "charactername", String.valueOf(transaction.getCharacterName()));
			}
			childNode.setAttributeNS(null, "stationid", String.valueOf(transaction.getStationID()));
			childNode.setAttributeNS(null, "stationname", String.valueOf(transaction.getStationName()));
			childNode.setAttributeNS(null, "transactiontype", String.valueOf(transaction.getTransactionType()));
			childNode.setAttributeNS(null, "transactionfor", String.valueOf(transaction.getTransactionFor()));
			//New
			childNode.setAttributeNS(null, "journaltransactionid", String.valueOf(transaction.getTransactionID()));
			childNode.setAttributeNS(null, "clienttypeid", String.valueOf(transaction.getClientID()));
			//Extra
			childNode.setAttributeNS(null, "accountkey", String.valueOf(transaction.getAccountKey()));
			node.appendChild(childNode);
		}
	}

	private void writeIndustryJobs(final Document xmldoc, final Element parentNode, final List<MyIndustryJob> industryJobs, final boolean bCorp) {
		Element node = xmldoc.createElementNS(null, "industryjobs");
		if (!industryJobs.isEmpty()) {
			node.setAttributeNS(null, "corp", String.valueOf(bCorp));
			parentNode.appendChild(node);
		}
		for (MyIndustryJob industryJob : industryJobs) {
			Element childNode = xmldoc.createElementNS(null, "industryjob");
			childNode.setAttributeNS(null, "jobid", String.valueOf(industryJob.getJobID()));
			childNode.setAttributeNS(null, "installerid", String.valueOf(industryJob.getInstallerID()));
			childNode.setAttributeNS(null, "installername", industryJob.getInstallerName());
			childNode.setAttributeNS(null, "facilityid", String.valueOf(industryJob.getFacilityID()));
			childNode.setAttributeNS(null, "solarsystemid", String.valueOf(industryJob.getSolarSystemID()));
			childNode.setAttributeNS(null, "solarsystemname", industryJob.getSolarSystemName());
			childNode.setAttributeNS(null, "stationid", String.valueOf(industryJob.getStationID()));
			childNode.setAttributeNS(null, "activityid", String.valueOf(industryJob.getActivityID()));
			childNode.setAttributeNS(null, "blueprintid", String.valueOf(industryJob.getBlueprintID()));
			childNode.setAttributeNS(null, "blueprinttypeid", String.valueOf(industryJob.getBlueprintTypeID()));
			childNode.setAttributeNS(null, "blueprinttypename", industryJob.getBlueprintTypeName());
			childNode.setAttributeNS(null, "blueprintlocationid", String.valueOf(industryJob.getBlueprintLocationID()));
			childNode.setAttributeNS(null, "outputlocationid", String.valueOf(industryJob.getOutputLocationID()));
			childNode.setAttributeNS(null, "runs", String.valueOf(industryJob.getRuns()));
			childNode.setAttributeNS(null, "cost", String.valueOf(industryJob.getCost()));
			childNode.setAttributeNS(null, "teamid", String.valueOf(industryJob.getTeamID()));
			childNode.setAttributeNS(null, "licensedruns", String.valueOf(industryJob.getLicensedRuns()));
			childNode.setAttributeNS(null, "probability", String.valueOf(industryJob.getProbability()));
			childNode.setAttributeNS(null, "producttypeid", String.valueOf(industryJob.getProductTypeID()));
			childNode.setAttributeNS(null, "producttypename", industryJob.getProductTypeName());
			childNode.setAttributeNS(null, "status", String.valueOf(industryJob.getStatus()));
			childNode.setAttributeNS(null, "timeinseconds", String.valueOf(industryJob.getTimeInSeconds()));
			childNode.setAttributeNS(null, "startdate", String.valueOf(industryJob.getStartDate().getTime()));
			childNode.setAttributeNS(null, "enddate", String.valueOf(industryJob.getEndDate().getTime()));
			childNode.setAttributeNS(null, "pausedate", String.valueOf(industryJob.getPauseDate().getTime()));
			childNode.setAttributeNS(null, "completeddate", String.valueOf(industryJob.getCompletedDate().getTime()));
			childNode.setAttributeNS(null, "completedcharacterid", String.valueOf(industryJob.getCompletedCharacterID()));
			node.appendChild(childNode);
		}
	}

	private void writeBlueprints(final Document xmldoc, final Element parentNode, final Map<Long, Blueprint> blueprints, final boolean bCorp) {
		Element node = xmldoc.createElementNS(null, "blueprints");
		if (!blueprints.isEmpty()) {
			node.setAttributeNS(null, "corp", String.valueOf(bCorp));
			parentNode.appendChild(node);
		}
		for (Blueprint blueprint : blueprints.values()) {
			Element childNode = xmldoc.createElementNS(null, "blueprint");
			childNode.setAttributeNS(null, "itemid", String.valueOf(blueprint.getItemID()));
			childNode.setAttributeNS(null, "locationid", String.valueOf(blueprint.getLocationID()));
			childNode.setAttributeNS(null, "typeid", String.valueOf(blueprint.getTypeID()));
			childNode.setAttributeNS(null, "typename", String.valueOf(blueprint.getTypeName()));
			childNode.setAttributeNS(null, "flagid", String.valueOf(blueprint.getFlagID()));
			childNode.setAttributeNS(null, "quantity", String.valueOf(blueprint.getQuantity()));
			childNode.setAttributeNS(null, "timeefficiency", String.valueOf(blueprint.getTimeEfficiency()));
			childNode.setAttributeNS(null, "materialefficiency", String.valueOf(blueprint.getMaterialEfficiency()));
			childNode.setAttributeNS(null, "runs", String.valueOf(blueprint.getRuns()));
			node.appendChild(childNode);
		}
	}
}
