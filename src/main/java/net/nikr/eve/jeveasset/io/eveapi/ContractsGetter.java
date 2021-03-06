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

package net.nikr.eve.jeveasset.io.eveapi;

import com.beimin.eveapi.exception.ApiException;
import com.beimin.eveapi.model.shared.Contract;
import com.beimin.eveapi.response.shared.ContractsResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.nikr.eve.jeveasset.data.MyAccount;
import net.nikr.eve.jeveasset.data.MyAccount.AccessMask;
import net.nikr.eve.jeveasset.data.Owner;
import net.nikr.eve.jeveasset.gui.dialogs.update.UpdateTask;
import net.nikr.eve.jeveasset.gui.tabs.contracts.MyContract;
import net.nikr.eve.jeveasset.gui.tabs.contracts.MyContractItem;
import net.nikr.eve.jeveasset.io.shared.AbstractApiGetter;
import net.nikr.eve.jeveasset.io.shared.ApiConverter;


public class ContractsGetter extends AbstractApiGetter<ContractsResponse>{

	public ContractsGetter() {
		super("Contracts", true, false);
	}

	public void load(UpdateTask updateTask, boolean forceUpdate, List<MyAccount> accounts) {
		super.loadAccounts(updateTask, forceUpdate, accounts);
	}

	@Override
	protected int getProgressStart() {
		return 0;
	}

	@Override
	protected int getProgressEnd() {
		return 30;
	}

	@Override
	protected ContractsResponse getResponse(boolean bCorp) throws ApiException {
		if (bCorp) {
			return new com.beimin.eveapi.parser.corporation.ContractsParser()
					.getResponse(Owner.getApiAuthorization(getOwner()));
		} else {
			return new com.beimin.eveapi.parser.pilot.ContractsParser()
					.getResponse(Owner.getApiAuthorization(getOwner()));
		}
	}

	@Override
	protected Date getNextUpdate() {
		return getOwner().getContractsNextUpdate();
	}

	@Override
	protected void setNextUpdate(Date nextUpdate) {
		getOwner().setContractsNextUpdate(nextUpdate);
	}

	@Override
	protected void setData(ContractsResponse response) {
		List<Contract> contracts = new ArrayList<Contract>(response.getAll());
		//Create backup of existin contracts
		//Map<MyContract, List<MyContractItem>> existingContract = new HashMap<MyContract, List<MyContractItem>>(getOwner().getContracts();
		// XXX - Workaround for ConcurrentModificationException in HashMap constructor
		Map<MyContract, List<MyContractItem>> existingContract = new HashMap<MyContract, List<MyContractItem>>();
		existingContract.putAll(getOwner().getContracts());
		//Remove existin contracts
		getOwner().getContracts().clear();

		//Create contract item cache (Optimization)
		Map<Long, List<MyContractItem>> contractItemsCache = new HashMap<Long, List<MyContractItem>>();
		for (Map.Entry<MyContract, List<MyContractItem>> entry : existingContract.entrySet()) {
			contractItemsCache.put(entry.getKey().getContractID(), entry.getValue());
		}

		for (Contract contract : contracts) {
			//Find existing contract items
			List<MyContractItem> existingContractItems = contractItemsCache.get(contract.getContractID());
			if (existingContractItems == null) {
				existingContractItems = new ArrayList<MyContractItem>();
			}
			//Convert contract
			MyContract myContract = ApiConverter.toContract(contract);
			//This is needed because we need to update the parent contract or the data will be incorrect
			List<MyContractItem> contractItems = ApiConverter.convertContractItems(existingContractItems, myContract);
			//Add
			getOwner().getContracts().put(myContract, contractItems);
		}
	}

	@Override
	protected void updateFailed(Owner ownerFrom, Owner ownerTo) {
		ownerTo.setContractsNextUpdate(ownerFrom.getContractsNextUpdate());
		//Clear existin
		ownerTo.getContracts().clear();
		//Set new
		ownerTo.getContracts().putAll(ownerFrom.getContracts());
	}

	@Override
	protected long requestMask(boolean bCorp) {
		if (bCorp) {
			return AccessMask.CONTRACTS_CORP.getAccessMask();
		} else {
			return AccessMask.CONTRACTS_CHAR.getAccessMask();
		}
	}
	
}
