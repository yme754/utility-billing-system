import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';
import { ConsumerService } from '../../../core/services/consumer';

declare var window: any;

@Component({
  selector: 'app-all-connections',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent, NavbarComponent],
  templateUrl: './all-connections.html',
  styleUrls: ['./all-connections.css']
})
export class OfficerAllConnectionsComponent implements OnInit {  
  allConnections: any[] = [];
  filteredConnections: any[] = [];  
  searchTerm: string = '';
  selectedStatus: string = 'ALL';
  selectedUtility: string = 'ALL';
  sortBy: string = 'date';
  isLoading = true;
  selectedConnection: any = null;
  actionType: string = '';
  isProcessing = false;
  actionSuccess = false;
  private modalInstance: any;

  constructor(
    private consumerService: ConsumerService,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadConnections();
  }

  loadConnections() {
    this.isLoading = true;
    this.consumerService.getAllConnections().subscribe({
      next: (data) => {
        this.allConnections = data;
        this.applyFilters();
        this.isLoading = false;
        this.cd.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load connections', err);
        this.isLoading = false;
      }
    });
  }

  openConfirmationModal(connection: any, type: string) {
    this.selectedConnection = connection;
    this.actionType = type;
    this.actionSuccess = false;
    this.isProcessing = false;

    const modalEl = document.getElementById('confirmationModal');
    if (modalEl && window.bootstrap) {
      this.modalInstance = new window.bootstrap.Modal(modalEl);
      this.modalInstance.show();
    }
  }

  confirmAction() {
    if (!this.selectedConnection) return;
    
    this.isProcessing = true;
    const newStatus = this.actionType === 'Activate' ? 'ACTIVE' : 'INACTIVE';

    this.consumerService.updateConnectionStatus(this.selectedConnection.id, newStatus).subscribe({
      next: () => {
        this.isProcessing = false;
        this.actionSuccess = true;        
        this.selectedConnection.status = newStatus;
        this.cd.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.isProcessing = false;
        alert('Action failed. Please try again.');
      }
    });
  }

  closeModal() {
    if (this.modalInstance) {
      this.modalInstance.hide();
    } else {
      const modalEl = document.getElementById('confirmationModal');
      const instance = window.bootstrap?.Modal?.getInstance(modalEl);
      if (instance) instance.hide();
    }
  }

  applyFilters() {
    let temp = [...this.allConnections];
    if (this.selectedStatus !== 'ALL') {
      temp = temp.filter(c => c.status === this.selectedStatus);
    }
    if (this.selectedUtility !== 'ALL') {
      temp = temp.filter(c => c.utilityType === this.selectedUtility);
    }
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      temp = temp.filter(c => 
        (c.consumerName && c.consumerName.toLowerCase().includes(term)) ||
        (c.meterNumber && c.meterNumber.toLowerCase().includes(term)) ||
        (c.id && c.id.toLowerCase().includes(term))
      );
    }
    if (this.sortBy === 'date') {
      temp.sort((a, b) => b.id.localeCompare(a.id)); 
    } else if (this.sortBy === 'name') {
      temp.sort((a, b) => (a.consumerName || '').localeCompare(b.consumerName || ''));
    }

    this.filteredConnections = temp;
  }
  
  filterByStatus(status: string) {
    this.selectedStatus = status;
    this.applyFilters();
  }

  getUtilityIcon(type: string): string {
    if (!type) return 'bi-question-circle';
    const t = type.toUpperCase();
    if (t === 'ELECTRICITY') return 'bi-lightning-charge-fill text-warning';
    if (t === 'WATER') return 'bi-droplet-fill text-info';
    if (t === 'GAS') return 'bi-fire text-danger';
    if (t === 'INTERNET') return 'bi-wifi text-primary';
    return 'bi-question-circle';
  }
}