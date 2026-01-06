import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';
import { BillingService } from '../../../core/services/billing';

declare var bootstrap: any;

@Component({
  selector: 'app-tariff-management',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent, NavbarComponent],
  templateUrl: './tariff-management.html',
  styleUrls: ['./tariff-management.css']
})
export class TariffManagementComponent implements OnInit {  
  allTariffs: any[] = [];
  filteredTariffs: any[] = [];
  isLoading = false;
  isSaving = false;
  isEditMode = false;
  searchTerm: string = '';
  selectedUtility: string = 'ALL';
  sortBy: string = 'name';
  currentTariff: any = {
    id: null,
    utilityType: 'ELECTRICITY',
    category: 'Residential',
    billingType: 'METERED',
    planName: '',
    description: '',
    baseRate: 0,
    slabs: []
  };
  tariffToDelete: any = null;

  constructor(
    private billingService: BillingService,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadTariffs();
  }

  loadTariffs() {
    this.isLoading = true;
    this.billingService.getAllTariffs().subscribe({
      next: (data) => {
        this.allTariffs = data || [];
        this.applyFilters();
        this.isLoading = false;
        this.cd.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load tariffs', err);
        this.isLoading = false;
      }
    });
  }

  applyFilters() {
    let temp = this.selectedUtility === 'ALL' 
      ? [...this.allTariffs] 
      : this.allTariffs.filter(t => t.utilityType === this.selectedUtility);
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      temp = temp.filter(t => 
        (t.planName && t.planName.toLowerCase().includes(term)) || 
        (t.category && t.category.toLowerCase().includes(term)) ||
        (t.utilityType && t.utilityType.toLowerCase().includes(term))
      );
    }
    if (this.sortBy === 'name') {
      temp.sort((a, b) => {
        const nameA = a.planName || a.category || '';
        const nameB = b.planName || b.category || '';
        return nameA.localeCompare(nameB);
      });
    } else if (this.sortBy === 'utility') {
      temp.sort((a, b) => a.utilityType.localeCompare(b.utilityType));
    }
    this.filteredTariffs = temp;
  }

  filterByUtility(type: string) {
    this.selectedUtility = type;
    this.applyFilters();
  }
  
  openCreateModal() {
    this.isEditMode = false;
    this.resetForm();
    this.showModal('tariffModal');
  }

  openEditModal(tariff: any) {
    this.isEditMode = true;
    this.currentTariff = JSON.parse(JSON.stringify(tariff)); 
    if (!this.currentTariff.slabs) this.currentTariff.slabs = [];
    this.showModal('tariffModal');
  }

  addSlab() {
    let start = 0;
    if (this.currentTariff.slabs.length > 0) {
      const lastSlab = this.currentTariff.slabs[this.currentTariff.slabs.length - 1];
      start = (lastSlab.maxUnits || 0) + 1;
    }
    this.currentTariff.slabs.push({ minUnits: start, maxUnits: 999999, rate: 0 });
  }

  removeSlab(index: number) {
    this.currentTariff.slabs.splice(index, 1);
  }

  onUtilityTypeChange() {
    const type = this.currentTariff.utilityType;
    if (type === 'ELECTRICITY' || type === 'WATER') {
      this.currentTariff.billingType = 'METERED';
      if (this.currentTariff.slabs.length === 0) this.addSlab();
    } else if (type === 'GAS') {
      this.currentTariff.billingType = 'ON_DEMAND';
      this.currentTariff.slabs = [];
    } else if (type === 'INTERNET') {
      this.currentTariff.billingType = 'SUBSCRIPTION';
      this.currentTariff.slabs = [];
    }
  }

  saveTariff() {
    this.isSaving = true;
    const payload = { ...this.currentTariff };
    if (payload.billingType !== 'METERED') payload.slabs = null;

    const request$ = this.isEditMode 
      ? this.billingService.updateTariff(payload.id, payload)
      : this.billingService.createTariff(payload);

    request$.subscribe({
      next: () => {
        this.isSaving = false;
        this.hideModal('tariffModal');
        this.loadTariffs();
      },
      error: (err) => {
        console.error('Error saving tariff:', err);
        this.isSaving = false;
      }
    });
  }

  confirmDelete(tariff: any) {
    this.tariffToDelete = tariff;
    this.showModal('deleteModal');
  }

  deleteTariff() {
    if (!this.tariffToDelete) return;
    this.billingService.deleteTariff(this.tariffToDelete.id).subscribe({
      next: () => {
        this.hideModal('deleteModal');
        this.loadTariffs();
        this.tariffToDelete = null;
      },
      error: (err) => console.error(err)
    });
  }

  resetForm() {
    this.currentTariff = {
      id: null,
      utilityType: 'ELECTRICITY',
      category: 'Residential',
      billingType: 'METERED',
      planName: '',
      description: '',
      baseRate: 0,
      slabs: []
    };
    this.onUtilityTypeChange();
  }

  getRateDisplay(plan: any): string {
    if (plan.billingType === 'METERED') {
      return `Tiered (${plan.slabs?.length || 0} Slabs)`;
    }
    return '₹' + (plan.baseRate || 0);
  }

  private showModal(id: string) {
    const el = document.getElementById(id);
    const modal = new bootstrap.Modal(el);
    modal.show();
  }
  
  private hideModal(id: string) {
    const el = document.getElementById(id);
    const modal = bootstrap.Modal.getInstance(el);
    if (modal) modal.hide();
  }
}