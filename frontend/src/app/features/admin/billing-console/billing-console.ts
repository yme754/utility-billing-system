import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { MeterService } from '../../../core/services/meter';
import { ConsumerService } from '../../../core/services/consumer';
import { BillingService } from '../../../core/services/billing';
import { SidebarComponent } from '../../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../../shared/components/navbar/navbar';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-billing-console',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, SidebarComponent, NavbarComponent],
  templateUrl: './billing-console.html',
  styleUrls: ['./billing-console.css']
})
export class BillingConsoleComponent implements OnInit {
  readingForm: FormGroup;  
  activeMeters: any[] = [];
  filteredMeters: any[] = [];
  selectedMeter: any = null;
  meterSearch: string = '';
  allBills: any[] = [];
  filteredBills: any[] = [];
  billSearch: string = '';
  activeTab: 'meters' | 'history' = 'meters'; 
  isLoading = false;
  message = '';
  isError = false;

  constructor(
    private fb: FormBuilder,
    private meterService: MeterService,
    private consumerService: ConsumerService,
    private billingService: BillingService,
    private cd: ChangeDetectorRef
  ) {
    this.readingForm = this.fb.group({
      reading: ['', [Validators.required, Validators.min(0)]]
    });
  }

  ngOnInit() {
    this.refreshDashboard();
  }

  refreshDashboard() {
    this.isLoading = true;
    forkJoin({
      connections: this.consumerService.getAllConnections(),
      bills: this.billingService.getAllBills()
    }).subscribe({
      next: ({ connections, bills }) => {        
        this.allBills = bills.map((bill: any) => {
          const conn = connections.find((c: any) => c.id === bill.connectionId);
          return {
            ...bill,
            consumerName: conn ? conn.consumerName : 'Unknown',
            utilityType: conn ? conn.utilityType : 'UNKNOWN'
          };
        });        
        this.activeMeters = connections
          .filter((c: any) => c.status && c.status.toUpperCase() === 'ACTIVE')
          .map((meter: any) => {
            const meterBills = this.allBills.filter(b => b.connectionId === meter.id);
            meterBills.sort((a, b) => new Date(b.billingDate).getTime() - new Date(a.billingDate).getTime());
            const lastBill = meterBills.length > 0 ? meterBills[0] : null;
            let isBillable = true;
            let daysRemaining = 0;
            let nextBillDate = null;
            if (lastBill) {
              const lastDate = new Date(lastBill.billingDate);
              const today = new Date();
              lastDate.setHours(0, 0, 0, 0);
              today.setHours(0, 0, 0, 0);              
              const diffTime = Math.abs(today.getTime() - lastDate.getTime());
              const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24)); 
              if (diffDays < 30) {
                isBillable = false;
                daysRemaining = 30 - diffDays;                
                const nextDate = new Date(lastDate);
                nextDate.setDate(lastDate.getDate() + 30);
                nextBillDate = nextDate;
              }
            }
            return {
              ...meter, isBillable, daysRemaining, nextBillDate,
              lastBillDate: lastBill ? lastBill.billingDate : null
            };
          });
        this.filterMeters(); 
        this.filterBills();
        this.isLoading = false;
        this.cd.detectChanges();
      },
      error: (err) => {
        console.error('Data Load Failed:', err);
        this.isLoading = false;
        this.showError('Failed to load data. Ensure backend is running.');
      }
    });
  }

  filterMeters() {
    if (!this.meterSearch.trim()) {
      this.filteredMeters = [...this.activeMeters];
    } else {
      const term = this.meterSearch.toLowerCase();
      this.filteredMeters = this.activeMeters.filter(m => 
        (m.consumerName && m.consumerName.toLowerCase().includes(term)) ||
        (m.meterNumber && m.meterNumber.toLowerCase().includes(term))
      );
    }
  }

  selectMeterForBilling(meter: any) {
    if (!meter.isBillable) return;
    this.selectedMeter = meter;
    this.readingForm.reset();
    if (meter.utilityType === 'INTERNET') {
      this.readingForm.get('reading')?.clearValidators();
      this.readingForm.get('reading')?.setValue(0);
    } else {
      this.readingForm.get('reading')?.setValidators([Validators.required, Validators.min(0)]);
      this.readingForm.get('reading')?.setValue('');
    }
    this.readingForm.get('reading')?.updateValueAndValidity();

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelSelection() {
    this.selectedMeter = null;
    this.readingForm.reset();
  }

  submitReading() {
    if (this.readingForm.invalid || !this.selectedMeter) return;
    this.isLoading = true;
    const readingValue = this.selectedMeter.utilityType === 'INTERNET' 
        ? 0 
        : Number(this.readingForm.value.reading);

    const readingPayload = {
      connectionId: this.selectedMeter.id,
      meterId: this.selectedMeter.meterNumber,
      reading: readingValue
    };

    this.meterService.submitReading(readingPayload).subscribe({
      next: (res) => {
        const billReq = {
          connectionId: this.selectedMeter.id,
          meterId: this.selectedMeter.meterNumber,
          utilityName: this.selectedMeter.utilityType,
          units: readingValue
        };
        this.billingService.generateBill(billReq).subscribe({
          next: () => {
            this.showSuccess(`Bill generated for ${this.selectedMeter.consumerName}`);
            this.selectedMeter = null; 
            this.refreshDashboard();
          },
          error: (err) => {
            this.isLoading = false;
            console.error(err);
            this.showError('Reading saved, but Bill Generation failed.');
          }
        });
      },
      error: (err) => {
        this.isLoading = false;
        console.error(err);
        const backendMsg = err.error?.message || 'Failed to submit reading.';
        this.showError(backendMsg);
      }
    });
  }

  filterBills() {
    if (!this.billSearch.trim()) {
      this.filteredBills = [...this.allBills];
    } else {
      const term = this.billSearch.toLowerCase();
      this.filteredBills = this.allBills.filter(b => 
        (b.id && b.id.toLowerCase().includes(term)) ||
        (b.consumerName && b.consumerName.toLowerCase().includes(term))
      );
    }
    this.filteredBills.sort((a, b) => new Date(b.billingDate).getTime() - new Date(a.billingDate).getTime());
  }
  
  getUtilityIcon(type: string): string {
    if (!type) return 'bi-question-circle';
    switch (type.toUpperCase()) {
      case 'ELECTRICITY': return 'bi-lightning-charge-fill text-warning';
      case 'WATER': return 'bi-droplet-fill text-info';
      case 'GAS': return 'bi-fire text-danger';
      case 'INTERNET': return 'bi-wifi text-primary';
      default: return 'bi-question-circle';
    }
  }

  getRowClass(meter: any) {
    return meter.isBillable ? '' : 'bg-light text-muted opacity-75';
  }

  private showSuccess(msg: string) {
    this.message = msg;
    this.isError = false;
    setTimeout(() => this.message = '', 3000);
  }
  private showError(msg: string) {
    this.message = msg;
    this.isError = true;
    this.cd.detectChanges();
  }
}